package net.focik.homeoffice.audit;

public class AsyncContext {
    private static final ThreadLocal<String> jobTypeHolder = new ThreadLocal<>();

    public static void setJobType(String jobType) {
        jobTypeHolder.set(jobType);
    }

    public static String getJobType() {
        return jobTypeHolder.get();
    }

    public static void clear() {
        jobTypeHolder.remove();
    }
}
