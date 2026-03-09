<template>
  <router-view />
</template>

<style>
/* 清除浏览器默认边距，让背景铺满 */
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  background-color: #f5f5f5;
}
#app {
  height: 100%;
}
</style>