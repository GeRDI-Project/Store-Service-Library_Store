package de.gerdiproject.store.tasks;

import de.gerdiproject.store.data.model.ResearchData;
import org.jctools.queues.SpmcArrayQueue;

import java.util.Queue;

public class TaskListenerFactory {

    private final Queue<ResearchData> queue;

    public TaskListenerFactory(Queue<ResearchData> queue) {
        this.queue = queue;
    }

    public final TaskListener createTaskListener(){
        return new TaskListener(queue);
    }
}
