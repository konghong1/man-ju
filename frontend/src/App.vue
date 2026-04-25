<template>
  <div class="shell">
    <header class="shell__header">
      <h1>Man-Ju Console</h1>
      <nav class="shell__nav">
        <RouterLink to="/">Projects</RouterLink>
        <RouterLink to="/projects/new">New</RouterLink>
        <RouterLink to="/projects/demo/workspace">Workspace</RouterLink>
        <RouterLink to="/projects/demo/review">Review</RouterLink>
        <RouterLink to="/projects/demo/export">Export</RouterLink>
        <RouterLink to="/settings/models">Models</RouterLink>
      </nav>
    </header>
    <main class="shell__content">
      <RouterView />
    </main>
  </div>
</template>
