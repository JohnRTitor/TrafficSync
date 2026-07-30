package trafficsync.terminal;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.AbstractListBox;
import com.googlecode.lanterna.gui2.TextGUIGraphics;

public class LogListBox extends AbstractListBox<Event, LogListBox> {

    public LogListBox(TerminalSize preferredSize) {
        super(preferredSize);
        setListItemRenderer(new ListItemRenderer<Event, LogListBox>() {
            @Override
            public int getHotSpotPositionOnLine(int index) {
                return -1;
            }

            @Override
            public String getLabel(LogListBox listBox, int index, Event item) {
                return item.getMessage();
            }

            @Override
            public void drawItem(TextGUIGraphics graphics, LogListBox listBox, int index, Event item, boolean selected, boolean focused) {
                TextColor color = TextColor.ANSI.DEFAULT;
                switch (item.getLevel()) {
                    case ERROR: color = TextColor.ANSI.RED; break;
                    case WARN: color = TextColor.ANSI.YELLOW; break;
                    case SNAPSHOT: color = TextColor.ANSI.BLUE; break;
                    case NETWORK: color = TextColor.ANSI.GREEN; break;
                    case USER: color = TextColor.ANSI.CYAN; break;
                }

                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
                String time = sdf.format(new java.util.Date(item.getTimestamp()));
                String levelText = String.format("%-8s", "[" + item.getLevel() + "]");
                String text = time + " " + levelText + " " + item.getMessage();

                graphics.setBackgroundColor(TextColor.ANSI.DEFAULT);
                graphics.setForegroundColor(color);

                if (selected) {
                    graphics.enableModifiers(SGR.REVERSE);
                }

                String padded = String.format("%-" + graphics.getSize().getColumns() + "s", text);
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
}
