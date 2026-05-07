package com.aiimage.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

@Service
public class MetadataExtractorService {

    private static final byte[] PNG_SIG = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    /**
     * Extracts AI generation metadata from an image file on disk.
     */
    public Map<String, String> extractMetadata(Path imagePath, String format) {
        Map<String, String> metadata = new HashMap<>();
        if ("png".equalsIgnoreCase(format)) {
            try (InputStream is = new BufferedInputStream(new FileInputStream(imagePath.toFile()))) {
                readPngTextChunks(is, metadata);
            } catch (Exception ignored) {
            }
        }
        return metadata;
    }

    /**
     * Extracts metadata from an InputStream without writing to disk.
     */
    public Map<String, String> extractMetadata(InputStream input, String format) {
        Map<String, String> metadata = new HashMap<>();
        if ("png".equalsIgnoreCase(format)) {
            try {
                // Read all bytes so we can mark/reset if needed (InputStream may not support mark)
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = input.read(buf)) != -1) {
                    baos.write(buf, 0, n);
                }
                readPngTextChunks(new ByteArrayInputStream(baos.toByteArray()), metadata);
            } catch (Exception ignored) {
            }
        }
        return metadata;
    }

    /**
     * Reads all tEXt and iTXt chunks from a PNG by parsing the raw binary.
     * This avoids truncation issues in javax.imageio.IIOMetadata.
     */
    private void readPngTextChunks(InputStream input, Map<String, String> metadata) throws IOException {
        DataInputStream data = new DataInputStream(input);

        // Verify PNG signature
        byte[] sig = new byte[8];
        data.readFully(sig);
        if (!Arrays.equals(sig, PNG_SIG)) return;

        while (true) {
            int chunkLen;
            try {
                chunkLen = data.readInt();
            } catch (EOFException e) {
                break;
            }

            byte[] typeBytes = new byte[4];
            data.readFully(typeBytes);
            String type = new String(typeBytes, StandardCharsets.US_ASCII);

            byte[] chunkData = new byte[chunkLen];
            if (chunkLen > 0) {
                data.readFully(chunkData);
            }

            // CRC (4 bytes) — skip
            data.readInt();

            if ("IEND".equals(type)) {
                break;
            }

            if ("tEXt".equals(type)) {
                parseTextChunk(chunkData, metadata);
            } else if ("iTXt".equals(type)) {
                parseITextChunk(chunkData, metadata);
            }
        }
    }

    private void parseTextChunk(byte[] data, Map<String, String> metadata) {
        // tEXt: keyword (ISO-8859-1, null-terminated) + value (ISO-8859-1)
        int nullPos = -1;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == 0) {
                nullPos = i;
                break;
            }
        }
        if (nullPos < 0) return;

        String keyword = new String(data, 0, nullPos, StandardCharsets.ISO_8859_1);
        String value = new String(data, nullPos + 1, data.length - nullPos - 1, StandardCharsets.ISO_8859_1);

        storeChunk(keyword, value, metadata);
    }

    private void parseITextChunk(byte[] data, Map<String, String> metadata) {
        // iTXt: keyword + null + compression_flag + compression_method + language_tag + null + translated_keyword + null + text
        int nullPos = -1;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == 0) {
                nullPos = i;
                break;
            }
        }
        if (nullPos < 0) return;

        String keyword = new String(data, 0, nullPos, StandardCharsets.ISO_8859_1);
        int pos = nullPos + 1;
        if (pos >= data.length) return;

        int compressionFlag = data[pos] & 0xFF;
        pos += 2; // skip compression_flag + compression_method
        if (pos >= data.length) return;

        // Skip language tag (null-terminated)
        while (pos < data.length && data[pos] != 0) pos++;
        pos++; // skip null
        if (pos >= data.length) return;

        // Skip translated keyword (null-terminated)
        while (pos < data.length && data[pos] != 0) pos++;
        pos++; // skip null
        if (pos >= data.length) return;

        byte[] textBytes = Arrays.copyOfRange(data, pos, data.length);
        String value;
        if (compressionFlag == 0) {
            value = new String(textBytes, StandardCharsets.UTF_8);
        } else {
            value = decompress(textBytes);
        }

        storeChunk(keyword, value, metadata);
    }

    private String decompress(byte[] data) {
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(buf);
                baos.write(buf, 0, count);
            }
        } catch (DataFormatException ignored) {
        } finally {
            inflater.end();
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    private void storeChunk(String keyword, String value, Map<String, String> metadata) {
        switch (keyword) {
            case "parameters":
            case "Description":
                parseA1111Metadata(value, metadata);
                break;
            case "prompt":
                // Store full raw JSON
                metadata.put("comfyuiPromptJson", value);
                parseComfyUIPrompt(value, metadata);
                break;
            case "workflow":
            case "workflowNodeLinks":
                metadata.put("workflowJson", value);
                break;
        }
        // Also store raw value so preview can show it
        metadata.put("raw_" + keyword, value);
    }

    private void parseA1111Metadata(String raw, Map<String, String> metadata) {
        try {
            String[] parts = raw.split("\nNegative prompt: ");
            if (parts.length > 0) {
                metadata.put("prompt", parts[0].trim());
            }
            if (parts.length > 1) {
                String remaining = parts[1];
                int negEnd = remaining.indexOf("\n");
                if (negEnd > 0) {
                    metadata.put("negativePrompt", remaining.substring(0, negEnd).trim());
                    remaining = remaining.substring(negEnd + 1);
                } else {
                    metadata.put("negativePrompt", "");
                }

                String[] params = remaining.split(",");
                for (String param : params) {
                    param = param.trim();
                    if (param.startsWith("Steps: ")) metadata.put("steps", param.substring(7).trim());
                    else if (param.startsWith("Sampler: ")) metadata.put("sampler", param.substring(9).trim());
                    else if (param.toLowerCase().contains("cfg scale")) {
                        metadata.put("cfgScale", param.substring(param.indexOf(": ") + 2).trim());
                    } else if (param.startsWith("Seed: ")) metadata.put("seed", param.substring(6).trim());
                    else if (param.startsWith("Model: ")) metadata.put("model", param.substring(7).trim());
                    else if (param.startsWith("Size: ")) {
                        String size = param.substring(6).trim();
                        String[] dims = size.split("x");
                        if (dims.length == 2) {
                            metadata.put("width", dims[0].trim());
                            metadata.put("height", dims[1].trim());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Parses ComfyUI prompt JSON node graph to extract generation parameters.
     * Supports SDXL (KSampler) and Flux (UltimateSDUpscale, UNETLoader) workflows.
     */
    private void parseComfyUIPrompt(String json, Map<String, String> metadata) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode nodes = mapper.readTree(json);
            if (!nodes.isObject()) return;

            // Find the primary sampler node — try KSampler first, then any node with sampler_name + denoise
            JsonNode samplerNode = findSamplerNode(nodes);
            if (samplerNode == null) return;

            JsonNode inputs = samplerNode.get("inputs");
            if (inputs == null) return;

            // Seed — may be literal or a node reference (e.g. rgthree Seed node).
            // KSamplerAdvanced uses "noise_seed" instead of "seed".
            if (inputs.has("seed")) {
                Long seed = resolveSeedRef(nodes, inputs.get("seed"), 0);
                if (seed != null) metadata.put("seed", String.valueOf(seed));
            }
            if (!metadata.containsKey("seed") && inputs.has("noise_seed")) {
                Long seed = resolveSeedRef(nodes, inputs.get("noise_seed"), 0);
                if (seed != null) metadata.put("seed", String.valueOf(seed));
            }
            if (inputs.has("steps")) metadata.put("steps", String.valueOf(inputs.get("steps").asInt()));
            if (inputs.has("cfg")) metadata.put("cfgScale", String.valueOf(inputs.get("cfg").asDouble()));
            if (inputs.has("sampler_name")) metadata.put("sampler", inputs.get("sampler_name").asText());
            if (inputs.has("denoise")) metadata.put("denoise", String.valueOf(inputs.get("denoise").asDouble()));

            // Follow references for prompts, model, dimensions
            if (inputs.has("positive")) {
                String text = resolveTextRef(nodes, inputs.get("positive"), 0);
                if (text != null) metadata.put("prompt", text);
            }
            if (inputs.has("negative")) {
                String text = resolveTextRef(nodes, inputs.get("negative"), 0);
                if (text != null) metadata.put("negativePrompt", text);
            }
            if (inputs.has("model")) {
                String model = resolveModelRef(nodes, inputs.get("model"), 0);
                if (model != null) metadata.put("model", model);
            }
            if (inputs.has("latent_image")) {
                String[] dims = resolveDimensionRef(nodes, inputs.get("latent_image"));
                if (dims != null) {
                    metadata.put("width", dims[0]);
                    metadata.put("height", dims[1]);
                }
            }

            // Also handle image input dimensions (img2img workflows)
            if (inputs.has("image")) {
                String[] imgDims = resolveImageDimensionRef(nodes, inputs.get("image"));
                if (imgDims != null && !metadata.containsKey("width")) {
                    metadata.put("width", imgDims[0]);
                    metadata.put("height", imgDims[1]);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Finds the most relevant sampler node in the workflow.
     * Priority: KSampler > KSamplerAdvanced > UltimateSDUpscale > any node with sampler_name
     */
    private JsonNode findSamplerNode(JsonNode nodes) {
        JsonNode fallback = null;
        Iterator<String> it = nodes.fieldNames();
        while (it.hasNext()) {
            String nodeId = it.next();
            JsonNode node = nodes.get(nodeId);
            String classType = node.has("class_type") ? node.get("class_type").asText() : "";

            if ("KSampler".equals(classType) || "KSamplerAdvanced".equals(classType)) {
                return node; // Prefer these
            }
            if ("UltimateSDUpscale".equals(classType)) {
                fallback = node; // Flux-style upscale workflows
            }
            // Generic fallback: any node with sampler_name AND denoise inputs
            if (fallback == null && node.has("inputs")) {
                JsonNode in = node.get("inputs");
                if (in.has("sampler_name") && (in.has("denoise") || in.has("steps"))) {
                    fallback = node;
                }
            }
        }
        return fallback;
    }

    /**
     * Follows reference chains through the node graph to find the prompt text.
     * Handles: CLIPTextEncode, CLIPTextEncodeSDXL, CR Text Concatenate, and chained references.
     */
    private String resolveTextRef(JsonNode nodes, JsonNode ref, int depth) {
        if (ref == null || depth > 8) return null;
        if (ref.isTextual()) return ref.asText();
        if (ref.isNumber()) return ref.asText();
        if (ref.isArray() && ref.size() >= 1) {
            String targetId = ref.get(0).asText();
            JsonNode targetNode = nodes.get(targetId);
            if (targetNode == null) return null;

            String classType = targetNode.has("class_type") ? targetNode.get("class_type").asText() : "";
            JsonNode inputs = targetNode.get("inputs");
            if (inputs == null) return null;

            // ConditioningZeroOut represents "no conditioning" — its input is the
            // positive prompt, so following it would produce a false negativePrompt
            if ("ConditioningZeroOut".equals(classType)) return null;

            // Direct text input (most common: "text" field)
            if (inputs.has("text")) {
                JsonNode textInput = inputs.get("text");
                if (textInput.isTextual()) return textInput.asText();
                if (textInput.isArray()) return resolveTextRef(nodes, textInput, depth + 1);
            }

            // Custom nodes like "CR Prompt Text" store prompt in a "prompt" field
            if (inputs.has("prompt") && inputs.get("prompt").isTextual()) {
                return inputs.get("prompt").asText();
            }

            // PrimitiveStringMultiline / PrimitiveString store text in a "value" field
            if (inputs.has("value") && inputs.get("value").isTextual()) {
                return inputs.get("value").asText();
            }

            // SDXL dual text encoders
            if (inputs.has("text_g") && inputs.get("text_g").isTextual()) {
                return inputs.get("text_g").asText();
            }

            // CR Text Concatenate: merge text1 + separator + text2
            if ("CR Text Concatenate".equals(classType)) {
                String text1 = null, text2 = null, separator = "";
                if (inputs.has("text1")) {
                    JsonNode t1 = inputs.get("text1");
                    text1 = t1.isTextual() ? t1.asText() : resolveTextRef(nodes, t1, depth + 1);
                }
                if (inputs.has("separator") && inputs.get("separator").isTextual()) {
                    separator = inputs.get("separator").asText();
                }
                if (inputs.has("text2")) {
                    JsonNode t2 = inputs.get("text2");
                    text2 = t2.isTextual() ? t2.asText() : resolveTextRef(nodes, t2, depth + 1);
                }
                if (text1 != null || text2 != null) {
                    return (text1 != null ? text1 : "") + separator + (text2 != null ? text2 : "");
                }
            }

            // Fallback: try following first array-type input to chase chains
            Iterator<String> fieldNames = inputs.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                JsonNode val = inputs.get(field);
                if (val.isArray() && val.size() >= 1 && val.get(0).isTextual()) {
                    String result = resolveTextRef(nodes, val, depth + 1);
                    if (result != null) return result;
                }
            }
        }
        return null;
    }

    /**
     * Follows reference chain to find model name.
     * Supports: CheckpointLoaderSimple (ckpt_name), UNETLoader (unet_name),
     * LoraLoaderModelOnly / LoraLoader, and patching chains.
     */
    private String resolveModelRef(JsonNode nodes, JsonNode ref, int depth) {
        if (ref == null || depth > 10) return null;
        if (ref.isTextual()) return ref.asText();
        if (ref.isArray() && ref.size() >= 1) {
            String targetId = ref.get(0).asText();
            JsonNode targetNode = nodes.get(targetId);
            if (targetNode == null) return null;

            String classType = targetNode.has("class_type") ? targetNode.get("class_type").asText() : "";
            JsonNode inputs = targetNode.get("inputs");
            if (inputs == null) return null;

            // Checkpoint (SDXL)
            if (inputs.has("ckpt_name")) {
                return stripExt(inputs.get("ckpt_name").asText());
            }
            // UNET (Flux)
            if (inputs.has("unet_name")) {
                return stripExt(inputs.get("unet_name").asText());
            }
            // Upscale model
            if (inputs.has("model_name")) {
                return inputs.get("model_name").asText();
            }
            // LoRA: read the lora_name and recurse through model
            if ("LoraLoaderModelOnly".equals(classType) || "LoraLoader".equals(classType)) {
                String loraName = inputs.has("lora_name") ? inputs.get("lora_name").asText() : null;
                String baseModel = null;
                if (inputs.has("model")) {
                    baseModel = resolveModelRef(nodes, inputs.get("model"), depth + 1);
                }
                if (loraName != null && baseModel != null) {
                    return baseModel + " + LoRA:" + stripExt(loraName);
                }
                if (baseModel != null) return baseModel;
                if (loraName != null) return "LoRA:" + stripExt(loraName);
            }
            // Recurse through model input
            if (inputs.has("model")) {
                return resolveModelRef(nodes, inputs.get("model"), depth + 1);
            }
        }
        return null;
    }

    /**
     * Resolves a seed value through node references (handles rgthree Seed node, etc.).
     */
    private Long resolveSeedRef(JsonNode nodes, JsonNode ref, int depth) {
        if (ref == null || depth > 5) return null;
        if (ref.isNumber()) return ref.asLong();
        if (ref.isArray() && ref.size() >= 1) {
            String targetId = ref.get(0).asText();
            JsonNode targetNode = nodes.get(targetId);
            if (targetNode == null) return null;
            JsonNode inputs = targetNode.get("inputs");
            if (inputs == null) return null;
            if (inputs.has("seed")) {
                return resolveSeedRef(nodes, inputs.get("seed"), depth + 1);
            }
        }
        return null;
    }

    /**
     * Follows reference to find dimensions from EmptyLatentImage.
     */
    private String[] resolveDimensionRef(JsonNode nodes, JsonNode ref) {
        if (ref == null || !ref.isArray() || ref.size() < 1) return null;
        String targetId = ref.get(0).asText();
        JsonNode targetNode = nodes.get(targetId);
        if (targetNode == null) return null;

        JsonNode inputs = targetNode.get("inputs");
        if (inputs == null) return null;

        if (inputs.has("width") && inputs.has("height")) {
            return new String[]{
                String.valueOf(inputs.get("width").asInt()),
                String.valueOf(inputs.get("height").asInt())
            };
        }
        return null;
    }

    /**
     * Follows reference to find image dimensions from a LoadImage node (img2img).
     */
    private String[] resolveImageDimensionRef(JsonNode nodes, JsonNode ref) {
        if (ref == null || !ref.isArray() || ref.size() < 1) return null;
        String targetId = ref.get(0).asText();
        JsonNode targetNode = nodes.get(targetId);
        if (targetNode == null) return null;

        String classType = targetNode.has("class_type") ? targetNode.get("class_type").asText() : "";
        if ("LoadImage".equals(classType)) {
            // TODO: we could read the image file to get actual dimensions,
            // but for now we just note it's an img2img workflow
            return null;
        }
        return null;
    }

    private String stripExt(String name) {
        if (name.endsWith(".safetensors")) return name.substring(0, name.length() - 12);
        if (name.endsWith(".ckpt")) return name.substring(0, name.length() - 5);
        if (name.endsWith(".pth")) return name.substring(0, name.length() - 4);
        return name;
    }
}
