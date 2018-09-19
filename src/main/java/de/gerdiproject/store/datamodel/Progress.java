package de.gerdiproject.store.datamodel;

import java.util.*;

public class Progress implements Iterable<TaskElement> {

    private final Map<String, TaskElement> map = new HashMap<>();

    Progress(List<String> docs) {
        for (String it : docs) {
            this.map.put(it, new TaskElement(it));
        }
    }

    public void setProgressOn(String id, Integer percent){
        if (percent > 100 || percent < 0) throw new IllegalArgumentException("Percent must be a value between 0 and 100");
        map.get(id).setProgressInPercent(percent);
    }

    @Override
    public Iterator<TaskElement> iterator() {
        return map.values().iterator();
    }

    public Collection<TaskElement> values() {
        return this.map.values();
    }
}
