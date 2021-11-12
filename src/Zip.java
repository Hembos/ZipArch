import java.io.*;
import java.util.Collections;

public class Zip {
    private enum Action {
        COMPRESS,
        DECOMPRESS;

        public static Action getAction(String action) {
            switch (action) {
                case "compression":
                    return COMPRESS;

                case "decompression":
                    return DECOMPRESS;

                default:
                    return null;
            }
        }
    }

    Config config;
    private String inputFile;
    private String outputFile;
    private Action action;
    private int bufferSize;

    Zip(Config config) {
        this.config = config;
    }

    private Error parseConfig() {
        inputFile = config.getArg(Config.Configuration.INPUT_FILE);

        action = Action.getAction(config.getArg(Config.Configuration.ACTION));
        if (action == null) {
            return Error.NOT_CORRECT_ACTION;
        }

        if (!config.getArg(Config.Configuration.OUTPUT_FILE).equals("")) {
            outputFile = config.getArg(Config.Configuration.OUTPUT_FILE);
        }

        try {
            bufferSize = Integer.parseInt(config.getArg(Config.Configuration.BUFFER_SIZE));
        } catch (NumberFormatException exception) {
            return Error.NOT_CORRECT_BUFFER_SIZE;
        }

        return null;
    }

    private Error compress() {
        try (FileInputStream fileInputStream = new FileInputStream(inputFile)) {
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);

            int available = fileInputStream.available();
            int a = available;

            while (available > 0) {
                System.out.println((100 - (float) available / a * 100) + "%");

                int curBlockSize = Math.min(bufferSize, available);
                byte[] bytes = new byte[curBlockSize];
                if (fileInputStream.read(bytes) == -1)
                    return Error.READ_ERROR;

                Compress compress = new Compress();

                compress.fillFrequencyTable(bytes);
                compress.createHuffmanTree();
                compress.createCodingTable();

                String tree = compress.getTreeText();
                String treeSize = String.format("%16s", Integer.toBinaryString(tree.length())).replace(" ", "0");
                StringBuilder res = new StringBuilder(treeSize + tree);

                for (byte b : bytes) {
                    res.append(compress.getCompressedByte(b));
                }

                byte countZeroInEnd = 0;
                if (res.length() % Compress.BYTE_SIZE != 0)
                    countZeroInEnd = (byte) (Compress.BYTE_SIZE - res.length() % Compress.BYTE_SIZE);

                res = new StringBuilder(String.format("%8s", Integer.toBinaryString(countZeroInEnd & compress.MASK)).replace(" ", "0") + res);

                res.append(String.join("", Collections.nCopies(countZeroInEnd, "0")));

                int countCompressedBytes = res.length() / Compress.BYTE_SIZE;
                res = new StringBuilder(String.format("%32s", Integer.toBinaryString(countCompressedBytes)).replace(" ", "0") +
                        String.format("%32s", Integer.toBinaryString(curBlockSize)).replace(" ", "0") + res);
                byte[] compressedBytes = new byte[countCompressedBytes + Compress.BYTE_SIZE];

                for (int i = 0; i < countCompressedBytes + Compress.BYTE_SIZE; i++) {
                    compressedBytes[i] = (byte) Integer.parseInt(res.substring(i * Compress.BYTE_SIZE, (i + 1) * Compress.BYTE_SIZE), 2);
                }

                fileOutputStream.write(compressedBytes);

                available -= bufferSize;
            }

        } catch (IOException exception) {
            return Error.COULD_NOT_OPEN_FILE;
        }

        return null;
    }

    //Количество сжатых байт (4 байта) + Количество исходных байтов + количество добавленных в конец нулей + размер дерева + дерево + сжатые данные + нули
    private Error decompress() {
        try (FileInputStream fileInputStream = new FileInputStream(inputFile)) {
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);

            int available = fileInputStream.available();
            int a = available;

            while (available != 0) {
                System.out.println((100 - (float) available / a * 100) + "%");
                int blockSize = 0;
                for (int i = 0; i < 4; i++) {
                    byte curByte = (byte) fileInputStream.read();

                    blockSize = blockSize | ((0xFF & curByte) << Decompress.BYTE_SIZE * (3 - i));
                }
                if (blockSize < 0) {
                    return Error.DECOMPRESS_ERROR;
                }

                int decompressedBlockSize = 0;
                for (int i = 0; i < 4; i++) {
                    byte curByte = (byte) fileInputStream.read();

                    decompressedBlockSize = decompressedBlockSize | ((0xFF & curByte) << Decompress.BYTE_SIZE * (3 - i));
                }

                byte[] block = new byte[blockSize];
                if (fileInputStream.read(block) == -1)
                    return Error.READ_ERROR;

                byte countZeroInEnd = block[0];

                short treeSize = (short) (((0xFF & block[1]) << Decompress.BYTE_SIZE) | (0xFF & block[2]));

                StringBuilder blockInTextBits = new StringBuilder();
                for (int i = 3; i < blockSize; i++) {
                    blockInTextBits.append(String.format("%8s", Integer.toBinaryString(block[i] & 0xff)).replace(" ", "0"));
                }

                String treeText = blockInTextBits.substring(0, treeSize);
                blockInTextBits = new StringBuilder(blockInTextBits.substring(treeSize));

                Decompress decompress = new Decompress(treeText);
                decompress.createHuffmanTree();
                decompress.createCodingTable();
                decompress.fillHufTable();

                blockInTextBits.delete(blockInTextBits.length() - countZeroInEnd, blockInTextBits.length());

                byte[] decompressedBlock = new byte[decompressedBlockSize];

                int decBlockIndex = 0;
                String character = "";
                for (int i = 0; i < blockInTextBits.length(); i++) {
                    character += blockInTextBits.charAt(i);
                    String decChar = decompress.getChar(character);
                    if (decChar != null) {
                        decompressedBlock[decBlockIndex] = (byte) Integer.parseInt(decChar, 2);
                        decBlockIndex++;
                        character = "";
                    }
                }
                if (!character.equals("")) {
                    return Error.DECOMPRESS_ERROR;
                }

                fileOutputStream.write(decompressedBlock, 0, decBlockIndex);

                available -= blockSize + 4;
            }


        } catch (IOException e) {
            return Error.COULD_NOT_OPEN_FILE;
        }

        return null;
    }

    public Error run() {
        Error error = parseConfig();
        if (error != null)
            return error;

        if (action == Action.COMPRESS) {
            error = compress();
        } else {
            error = decompress();
        }

        return error;
    }
}
