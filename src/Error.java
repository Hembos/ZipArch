public enum Error {
    NOT_CORRECT_CONFIG_ARGUMENTS("ERROR: Not correct config"),
    COULD_NOT_OPEN_FILE("ERROR: File not open"),
    NOT_CORRECT_ACTION("ERROR: In config file not correct action"),
    NOT_CORRECT_BUFFER_SIZE("ERROR: Incorrect buffer size in config file"),
    DECOMPRESS_ERROR("ERROR: Decompressing error"),
    READ_ERROR("ERROR: Could not read file");

    private final String errorMessage;

    Error(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
