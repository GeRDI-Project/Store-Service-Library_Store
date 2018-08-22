package de.gerdiproject.store;

import de.gerdiproject.store.data.model.ResearchData;
import de.gerdiproject.store.kafka.GerdiKafkaConsumer;
import de.gerdiproject.store.tasks.TaskListener;
import de.gerdiproject.store.tasks.TaskListenerFactory;
import org.jctools.queues.SpmcArrayQueue;

import java.util.Queue;

public class StoreConfigurator {


    private boolean inited = false;

    private final GerdiKafkaConsumer gerdiKafkaConsumer;
    private final Queue<ResearchData> queue = new SpmcArrayQueue<ResearchData>(64); // Optimization: Use power of two
    private final TaskListenerFactory taskListenerFactory = new TaskListenerFactory(this.queue);

    public StoreConfigurator(String id){
        if (this.inited) throw new IllegalStateException("StoreConfigurator must only be instantiated once");
        this.gerdiKafkaConsumer = new GerdiKafkaConsumer(this.queue, id);
        this.gerdiKafkaConsumer.start();
        // TODO: register service at GeRDI
        this.inited = true;
    }

    public TaskListener init(String id){
        return this.taskListenerFactory.createTaskListener();
    }



}
