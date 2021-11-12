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
    private final byte BYTE_SIZE = 8;
    private final int MIN_BUFFER_SIZE = 100;
    private final int MAX_BUFFER_SIZE = 1000000;

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
            if (bufferSize < MIN_BUFFER_SIZE || bufferSize > MAX_BUFFER_SIZE) {
                return Error.NOT_CORRECT_BUFFER_SIZE;
            }
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
                fileInputStream.read(bytes);

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
                if (res.length() % BYTE_SIZE != 0)
                    countZeroInEnd = (byte) (BYTE_SIZE - res.length() % BYTE_SIZE);

                res = new StringBuilder(String.format("%8s", Integer.toBinaryString(countZeroInEnd & compress.MASK)).replace(" ", "0") + res);

                res.append(String.join("", Collections.nCopies(countZeroInEnd, "0")));

                int countCompressedBytes = res.length() / BYTE_SIZE;
                res = new StringBuilder(String.format("%32s", Integer.toBinaryString(countCompressedBytes)).replace(" ", "0") + res);
                byte[] compressedBytes = new byte[countCompressedBytes + 4];

                for (int i = 0; i < countCompressedBytes + 4; i++) {
                    compressedBytes[i] = (byte) Integer.parseInt(res.substring(i * BYTE_SIZE, (i + 1) * BYTE_SIZE), 2);
                }

                fileOutputStream.write(compressedBytes);

                available -= bufferSize;
            }

        } catch (IOException exception) {
            return Error.COULD_NOT_OPEN_FILE;
        }

        return null;
    }

    //Количество сжатых байт (4 байта) + /*Количество исходных байт*/ + количество добавленных в конец нулей + размер дерева + дерево + сжатые данные + нули
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

                byte[] block = new byte[blockSize];
                fileInputStream.read(block);

                byte countZeroInEnd = block[0];

                short treeSize = (short) (((0xFF & block[1]) << Decompress.BYTE_SIZE) | (0xFF & block[2]));

                StringBuilder blockInTextBits = new StringBuilder("");
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

                byte[] decompressedBlock = new byte[MAX_BUFFER_SIZE + treeSize];

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
