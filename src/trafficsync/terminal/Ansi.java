package trafficsync.terminal;

public class Ansi {
    public static final String ESC = "\033[";
    
    // Clear ops
    public static final String CLEAR_SCREEN = ESC + "2J";
    public static final String CLEAR_LINE = ESC + "2K";
    
    // Cursor ops
    public static final String CURSOR_HOME = ESC + "H";
    public static final String HIDE_CURSOR = ESC + "?25l";
    public static final String SHOW_CURSOR = ESC + "?25h";
    
    public static String cursorTo(int row, int col) {
        return ESC + row + ";" + col + "H";
    }

    // Colors
    public static final String RESET = ESC + "0m";
    public static final String BOLD = ESC + "1m";
    
    public static final String RED = ESC + "31m";
    public static final String GREEN = ESC + "32m";
    public static final String YELLOW = ESC + "33m";
    public static final String BLUE = ESC + "34m";
    public static final String CYAN = ESC + "36m";
    public static final String WHITE = ESC + "37m";
}
