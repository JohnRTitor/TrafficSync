package aggregator;

import snapshot.SnapshotReport;
import snapshot.LocalState;
import snapshot.ChannelState;
import common.Message;
import common.MessageType;

import java.util.List;

public class SnapshotSerializer {
    
    public static String serialize(SnapshotReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        LocalState local = report.getLocalState();
        sb.append("\"nodeId\":").append(local.getNodeId()).append(",");
        sb.append("\"sentMessages\":").append(local.getSentMessages()).append(",");
        sb.append("\"receivedMessages\":").append(local.getReceivedMessages()).append(",");
        
        sb.append("\"events\":[");
        List<String> events = local.getEvents();
        for (int i = 0; i < events.size(); i++) {
            sb.append("\"").append(events.get(i).replace("\"", "\\\"")).append("\"");
            if (i < events.size() - 1) sb.append(",");
        }
        sb.append("],");
        
        sb.append("\"channelStates\":[");
        List<ChannelState> channels = report.getChannelStates();
        for (int i = 0; i < channels.size(); i++) {
            ChannelState cs = channels.get(i);
            sb.append("{");
            sb.append("\"channelId\":").append(cs.getSourceNodeId()).append(",");
            sb.append("\"messages\":[");
            List<Message> msgs = cs.getMessages();
            for (int j = 0; j < msgs.size(); j++) {
                Message m = msgs.get(j);
                sb.append("{");
                sb.append("\"type\":\"").append(m.getType().name()).append("\",");
                sb.append("\"senderId\":").append(m.getSenderId()).append(",");
                sb.append("\"receiverId\":").append(m.getReceiverId()).append(",");
                sb.append("\"payload\":\"").append(m.getPayload().replace("\"", "\\\"")).append("\"");
                sb.append("}");
                if (j < msgs.size() - 1) sb.append(",");
            }
            sb.append("]}");
            if (i < channels.size() - 1) sb.append(",");
        }
        sb.append("]");
        sb.append("}");
        
        return sb.toString();
    }
    
    public static SnapshotReport deserialize(String json) {
        int nodeId = extractInt(json, "\"nodeId\":");
        LocalState local = new LocalState(nodeId);
        
        int sentMessages = extractInt(json, "\"sentMessages\":");
        int receivedMessages = extractInt(json, "\"receivedMessages\":");
        
        for (int i = 0; i < sentMessages; i++) local.incrementSentMessages();
        for (int i = 0; i < receivedMessages; i++) local.incrementReceivedMessages();
        
        int eventsStart = json.indexOf("\"events\":[") + 10;
        int eventsEnd = json.indexOf("],", eventsStart);
        if (eventsStart != -1 && eventsEnd != -1 && eventsStart < eventsEnd) {
            String eventsStr = json.substring(eventsStart, eventsEnd);
            if (!eventsStr.isBlank()) {
                String[] evs = eventsStr.split("\",\"");
                for (String ev : evs) {
                    local.addEvent(ev.replace("\"", "").replace("\\\"", "\""));
                }
            }
        }
        
        SnapshotReport report = new SnapshotReport(local);
        
        int channelsStart = json.indexOf("\"channelStates\":[") + 17;
        int channelsEnd = json.lastIndexOf("]");
        if (channelsStart != -1 && channelsEnd != -1 && channelsStart < channelsEnd) {
            String channelsStr = json.substring(channelsStart, channelsEnd);
            int idx = 0;
            while ((idx = channelsStr.indexOf("{\"channelId\":", idx)) != -1) {
                int endIdx = channelsStr.indexOf("]}", idx);
                if (endIdx == -1) break;
                String channelJson = channelsStr.substring(idx, endIdx + 2);
                int channelId = extractInt(channelJson, "\"channelId\":");
                ChannelState cs = new ChannelState(channelId);
                
                int msgsStart = channelJson.indexOf("\"messages\":[") + 12;
                int msgsEnd = channelJson.indexOf("]}", msgsStart);
                if (msgsStart != -1 && msgsEnd != -1 && msgsStart < msgsEnd) {
                    String msgsStr = channelJson.substring(msgsStart, msgsEnd);
                    int msgIdx = 0;
                    while ((msgIdx = msgsStr.indexOf("{", msgIdx)) != -1) {
                        int mEnd = msgsStr.indexOf("}", msgIdx);
                        if (mEnd == -1) break;
                        String mStr = msgsStr.substring(msgIdx, mEnd + 1);
                        String typeStr = extractString(mStr, "\"type\":\"");
                        int senderId = extractInt(mStr, "\"senderId\":");
                        int receiverId = extractInt(mStr, "\"receiverId\":");
                        String payload = extractString(mStr, "\"payload\":\"").replace("\\\"", "\"");
                        
                        Message msg = new Message(MessageType.valueOf(typeStr), senderId, receiverId, payload);
                        cs.recordMessage(msg);
                        
                        msgIdx = mEnd + 1;
                    }
                }
                report.addChannelState(cs);
                idx = endIdx + 2;
            }
        }
        
        return report;
    }
    
    private static int extractInt(String json, String key) {
        int index = json.indexOf(key);
        if (index == -1) return 0;
        int start = index + key.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return Integer.parseInt(json.substring(start, end).trim());
    }
    
    private static String extractString(String json, String key) {
        int index = json.indexOf(key);
        if (index == -1) return "";
        int start = index + key.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
