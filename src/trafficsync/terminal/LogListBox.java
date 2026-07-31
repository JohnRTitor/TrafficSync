package trafficsync.terminal;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.AbstractListBox;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

// This is a custom UI component built on top of Lanterna's AbstractListBox.
// The standard list box only supports plain text, so we extend it to support
// color-coded log entries, timestamps, and horizontal scrolling. The rendering
// logic is entirely custom -- each row is drawn with a color that matches the
// event's severity level (e.g., red for errors, blue for snapshots).
public class LogListBox extends AbstractListBox<Event, LogListBox> {
    // We keep track of how far the user has scrolled to the right.
    // This is needed because long messages might not fit on a single terminal line.
    private int horizontalScroll = 0;

    // We override the default drawing method so we can add colors and timestamps.
    public LogListBox(TerminalSize preferredSize) {
        super(preferredSize);
        setListItemRenderer(new ListItemRenderer<Event, LogListBox>() {
            // Returning -1 disables the hotspot cursor for list items.
            // We do not need a cursor indicator because we use reverse-video
            // highlighting for the selected row instead.
            @Override
            public int getHotSpotPositionOnLine(int index) {
                return -1;
            }

            @Override
            public String getLabel(LogListBox listBox, int index, Event item) {
                return item.getMessage();
            }

            @Override
            public void drawItem(
                    TextGUIGraphics graphics,
                    LogListBox listBox,
                    int index,
                    Event item,
                    boolean selected,
                    boolean focused) {
                // We change the text color based on how important the event is.
                TextColor color =
                        switch (item.getLevel()) {
                            case ERROR -> TextColor.ANSI.RED;
                            case WARN -> TextColor.ANSI.YELLOW;
                            case SNAPSHOT -> TextColor.ANSI.BLUE;
                            case NETWORK -> TextColor.ANSI.GREEN;
                            case USER -> TextColor.ANSI.CYAN;
                            default -> TextColor.ANSI.DEFAULT;
                        };

                // Build the full log line: timestamp + severity tag + message text.
                // The level text is padded to 8 characters so all the messages line up neatly.
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
                String time = sdf.format(new java.util.Date(item.getTimestamp()));
                String levelText = String.format("%-8s", "[" + item.getLevel() + "]");
                String text = time + " " + levelText + " " + item.getMessage();

                // This part handles the sideways scrolling. If the user scrolled,
                // we chop off the beginning of the text so the rest fits on the screen.
                int scroll = listBox.getHorizontalScroll();
                String textToDraw = text;
                if (scroll > 0) {
                    if (scroll < text.length()) {
                        textToDraw = text.substring(scroll);
                    } else {
                        textToDraw = "";
                    }
                }

                graphics.setBackgroundColor(TextColor.ANSI.DEFAULT);
                graphics.setForegroundColor(color);

                if (selected) {
                    graphics.enableModifiers(SGR.REVERSE);
                }

                // Pad the text to fill the entire terminal width, then truncate if it is
                // still too long. This prevents leftover characters from a previous draw
                // from showing through when a shorter line replaces a longer one.
                String padded = String.format("%-" + graphics.getSize().getColumns() + "s", textToDraw);
                if (padded.length() > graphics.getSize().getColumns()) {
                    padded = padded.substring(0, graphics.getSize().getColumns());
                }
                graphics.putString(0, 0, padded);

                if (selected) {
                    graphics.disableModifiers(SGR.REVERSE);
                }
            }
        });
    }

    // Returns the current horizontal scroll offset.
    // The renderer reads this value during drawItem to decide how much text to skip.
    public int getHorizontalScroll() {
        return horizontalScroll;
    }

    // We listen for the left and right arrow keys and shift the scroll offset by 5 columns.
    // Calling invalidate() tells Lanterna that the component content has changed
    // and needs to be redrawn on the next rendering pass.

    @Override
    public Interactable.Result handleKeyStroke(KeyStroke keyStroke) {
        if (keyStroke.getKeyType() == KeyType.ArrowRight) {
            horizontalScroll += 5;
            invalidate();
            return Interactable.Result.HANDLED;
        } else if (keyStroke.getKeyType() == KeyType.ArrowLeft) {
            horizontalScroll = Math.max(0, horizontalScroll - 5);
            invalidate();
            return Interactable.Result.HANDLED;
        }
        return super.handleKeyStroke(keyStroke);
    }
}
