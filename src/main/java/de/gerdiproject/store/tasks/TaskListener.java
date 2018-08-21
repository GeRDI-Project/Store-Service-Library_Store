package de.gerdiproject.store.tasks;

import de.gerdiproject.store.data.model.ResearchData;

import java.util.Queue;

public class TaskListener {

    private final Queue<ResearchData> queue;

    TaskListener(Queue queue){
        this.queue = queue;
    }

    public ResearchData poll(){
        ResearchData element = null;
        do {
            element = this.queue.poll();
        } while (element == null); // Busy waiting
        return element;
    }

}
