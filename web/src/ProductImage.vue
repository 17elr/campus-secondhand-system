<template>
  <div class="product-photo" :role="failed||!src?'img':undefined" :aria-label="failed||!src?alt+'（暂无图片）':undefined"><img v-if="src&&!failed" :src="src" :alt="alt" :loading="lazy?'lazy':'eager'" @error="failed=true"/><div v-else class="photo-empty"><el-icon><Picture/></el-icon><span>暂无商品照片</span></div></div>
</template>
<script setup>
import {ref, watch} from 'vue'
import {Picture} from '@element-plus/icons-vue'
const props=defineProps({src:String,alt:{type:String,default:'商品'},lazy:Boolean})
const failed=ref(false)
watch(()=>props.src,()=>{failed.value=false})
</script>
<style scoped>
.product-photo{width:100%;height:100%;background:#fff;overflow:hidden;border-radius:inherit}.product-photo img{display:block;width:100%;height:100%;object-fit:contain}.photo-empty{width:100%;height:100%;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:6px;background:#f1f4ed;color:#7d8c73}.photo-empty .el-icon{font-size:26px}.photo-empty span{font-size:11px}
</style>
