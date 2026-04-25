import { defineStore } from "pinia";

export type JobStatus = "queued" | "running" | "succeeded" | "failed";

export type JobState = {
  id: string;
  projectId: string;
  status: JobStatus;
  updatedAt: string;
};

type JobsState = {
  byId: Record<string, JobState>;
};

export const useJobStore = defineStore("job", {
  state: (): JobsState => ({
    byId: {},
  }),
  getters: {
    jobs(state) {
      return Object.values(state.byId);
    },
  },
  actions: {
    upsertJob(job: JobState) {
      this.byId[job.id] = job;
    },
    clearJobs() {
      this.byId = {};
    },
  },
});
