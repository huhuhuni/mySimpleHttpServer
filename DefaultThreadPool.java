package myHttpServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultThreadPool<Job extends Runnable> {
    //线程池最大限制数
    private static final int MAX_WORKER_NUMBERS = 10;
    //线程池默认的数量
    private static final int DEFAULT_WORKER_NUMBERS = 5;
    //线程池最小数量
    private static final int MIN_WORKER_NUMBERS = 1;
    //工作列表
    private final LinkedList<Job> jobs = new LinkedList<Job>();
    //工作者列表
    private final List<Worker> workers = Collections.synchronizedList(new ArrayList<Worker>());
    //工作者线程数量
    private int workNum = DEFAULT_WORKER_NUMBERS;
    //线程编号生成
    private AtomicLong threadNum = new AtomicLong();

    public DefaultThreadPool(){
        initializeWorkers(DEFAULT_WORKER_NUMBERS);
    }

    public DefaultThreadPool(int num){
        workNum = num > MAX_WORKER_NUMBERS ? MAX_WORKER_NUMBERS : num < MIN_WORKER_NUMBERS ? MIN_WORKER_NUMBERS : num;
        initializeWorkers(DEFAULT_WORKER_NUMBERS);
    }

    public void execute(Job job) {
        if(job != null){
            synchronized(jobs){
                jobs.addLast(job);
                jobs.notify();
            }
        }
    }
    public void shutdown(){
        for(Worker worker : workers){
            worker.shutdown();
        }
    }

    public void addWorkers(int num){
        synchronized (jobs) {
            //限制新增的Worker数量不能超过最大值
            if(num + this.workNum >MAX_WORKER_NUMBERS) {
                num = MAX_WORKER_NUMBERS - this.workNum;
            }
            initializeWorkers(num);
            this.workNum += num;
        }
    }
    public void removeWorker(int num){
        synchronized (jobs) {
            if(num >= this.workNum) {
                throw new IllegalArgumentException("beyond workNum");
            }
            //按照数量停止worker
            int count = 0;
            while (count < num) {
                Worker worker = workers.get(count);
                if (workers.remove(worker)) {
                    worker.shutdown();
                    count++;
                }
            }
            this.workNum -= count;
        }
    }

    public int getJobSize() {
        return jobs.size();
    }
    //初始化线程工作者
    private void initializeWorkers(int num){
        for (int i = 0; i<num; i++){
            Worker worker = new Worker();
            Thread thread = new Thread(worker,"ThreadPool-Worker-" + threadNum.incrementAndGet());
            thread.start();
        }
    }
    //工作者，负责消费任务
    class Worker implements Runnable {
        //是否工作
        private volatile boolean running = true;

        @Override
        public void run() {
            while(running) {
                Job job = null;
                synchronized (jobs) {
                    //如果工作者列表是空的，那么就wait
                    while (jobs.isEmpty()){
                        try {
                            jobs.wait();
                        } catch (InterruptedException e) {
                            //e.printStackTrace();
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    job = jobs.removeFirst();
                }
                if(job != null){
                    try {
                        job.run();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
        public void shutdown(){
            running = false;
        }
    }


}