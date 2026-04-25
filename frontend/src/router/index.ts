import { createRouter, createWebHistory } from "vue-router";

import ExportView from "../views/ExportView.vue";
import ModelSettingsView from "../views/ModelSettingsView.vue";
import NewProjectView from "../views/NewProjectView.vue";
import NotFoundView from "../views/NotFoundView.vue";
import ProjectListView from "../views/ProjectListView.vue";
import ReviewView from "../views/ReviewView.vue";
import WorkspaceView from "../views/WorkspaceView.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      name: "projects",
      component: ProjectListView,
    },
    {
      path: "/projects/new",
      name: "project-new",
      component: NewProjectView,
    },
    {
      path: "/projects/:id/workspace",
      name: "project-workspace",
      component: WorkspaceView,
      props: true,
    },
    {
      path: "/projects/:id/review",
      name: "project-review",
      component: ReviewView,
      props: true,
    },
    {
      path: "/projects/:id/export",
      name: "project-export",
      component: ExportView,
      props: true,
    },
    {
      path: "/settings/models",
      name: "model-settings",
      component: ModelSettingsView,
    },
    {
      path: "/:pathMatch(.*)*",
      name: "not-found",
      component: NotFoundView,
    },
  ],
});

export default router;
