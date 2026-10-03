
import os
import glob

f = "src/main/java/net/map6b6t/network/UploadService.java"
with open(f, "r") as file:
    content = file.read()

# Add submitAsyncScan right above submitRawPacketAsync
method_to_add = """
    public void submitAsyncScan(
            String dimension,
            int chunkX,
            int chunkZ,
            String playerName,
            String serverVersion,
            net.map6b6t.scanner.PrimitiveChunkSnapshot snapshot,
            java.util.function.BiConsumer<Long, Integer> onHashComputed
    ) {
        if (!running.get() || scanExecutor == null || scanExecutor.isShutdown()) {
            return;
        }
        scanExecutor.execute(() -> {
            try {
                int contentHash = snapshot.hashCode();
                long chunkKey = (((long) chunkX) & 0xFFFFFFFFL) | ((((long) chunkZ) & 0xFFFFFFFFL) << 32);
                if (onHashComputed != null) {
                    onHashComputed.accept(chunkKey, contentHash);
                }

                ChunkSubmission job = new ChunkSubmission(
                        dimension,
                        chunkX,
                        chunkZ,
                        playerName,
                        serverVersion,
                        snapshot,
                        contentHash
                );
                
                queue.offer(job);
                stats.incrementTracked();
            } catch (Exception e) {
                LOGGER.error("Failed async chunk scan", e);
            }
        });
    }
"""

if "submitAsyncScan(" not in content:
    content = content.replace("public void submitRawPacketAsync(", method_to_add + "\n    public void submitRawPacketAsync(")
    with open(f, "w") as file:
        file.write(content)
    print("Fixed src/main UploadService")

# Also fix the versions/ directories
version_files = glob.glob("versions/*/src/main/java/net/map6b6t/network/UploadService.java")
for vf in version_files:
    with open(vf, "r") as file:
        vcontent = file.read()
    if "submitAsyncScan(" not in vcontent:
        vcontent = vcontent.replace("public void submitRawPacketAsync(", method_to_add + "\n    public void submitRawPacketAsync(")
        with open(vf, "w") as file:
            file.write(vcontent)
        print("Fixed " + vf)

