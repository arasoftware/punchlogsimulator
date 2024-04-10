package com.example.functionality;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class MessageSimulationQueue {
    private HashMap<String, Integer> recordIdMap;
    private List<String> messageQueue;

    public MessageSimulationQueue() {
        recordIdMap = new HashMap<>();
        messageQueue = new ArrayList<String>();

    }

    public void removeMessage(String recordId) {
        int index = recordIdMap.getOrDefault(recordId, -1);
        if (index == -1) {
            return;
        }
        messageQueue.remove(index);
        recordIdMap.remove(recordId);
    }

    public void addMessage(String recordId, String message) {
        messageQueue.add(message);
        int index = messageQueue.size() - 1;
        recordIdMap.put(recordId, index);
    }

    public Optional<String> getLatestMessage() {
        int index =messageQueue.size()-1;
        if(index ==-1 ){

          return  Optional.of(null);
        }
       return Optional.of( messageQueue.get(index));
    }
}
