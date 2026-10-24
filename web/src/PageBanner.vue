<template>
  <section :class="['page-banner',{'banner-compact':compact||admin}]" role="region" aria-roledescription="轮播" aria-label="校园主题轮播" tabindex="0" @keydown.left.prevent="carousel?.prev()" @keydown.right.prevent="carousel?.next()">
    <el-carousel ref="carousel" :height="compact||admin?'240px':'380px'" :autoplay="!paused&&!hidden" :interval="3500" :pause-on-hover="false" arrow="always" indicator-position="none" @change="active=$event">
      <el-carousel-item v-for="(slide,index) in slides" :key="page+'-'+index">
        <div class="banner-scene" :aria-hidden="active!==index">
          <img :src="slide.image" :alt="slide.alt" class="banner-picture" :loading="index===0?'eager':'lazy'"/>
          <div class="banner-shade"></div>
          <div class="banner-copy"><p class="banner-eyebrow">{{slide.eyebrow}}</p><component :is="page==='market'&&!admin?'h1':'h2'">{{slide.title}}<br><em>{{slide.highlight}}</em></component><p class="banner-description">{{slide.description}}</p><button class="button banner-action" :tabindex="active===index?0:-1" @click="$emit('action',slide.target)">{{slide.action}}<el-icon><ArrowRight/></el-icon></button></div>
        </div>
      </el-carousel-item>
    </el-carousel>
    <div class="banner-controls"><span class="banner-number">{{String(active+1).padStart(2,'0')}} / {{String(slides.length).padStart(2,'0')}}</span><div class="banner-dots"><button v-for="(slide,index) in slides" :key="index" :class="{active:active===index}" :aria-label="'切换到第 '+(index+1)+' 张：'+slide.title" :aria-pressed="active===index" @click="carousel?.setActiveItem(index)"></button></div><button class="banner-pause" :aria-label="paused?'播放轮播':'暂停轮播'" :aria-pressed="paused" @click="paused=!paused"><el-icon><VideoPlay v-if="paused"/><VideoPause v-else/></el-icon><span>{{paused?'播放':'暂停'}}</span></button></div>
  </section>
</template>

<script setup>
import {computed, onMounted, onUnmounted, ref, watch} from 'vue'
import {ArrowRight, VideoPause, VideoPlay} from '@element-plus/icons-vue'
const props=defineProps({page:{type:String,default:'market'},admin:Boolean,compact:Boolean})
defineEmits(['action'])
const carousel=ref(null),active=ref(0),paused=ref(false),hidden=ref(false)
const bannerKey=computed(()=>props.admin?'admin-'+props.page:props.page)
const themes={
  market:{eyebrow:'CAMPUS STORIES · 拾光集市',title:'闲置有去处，',highlight:'好物有新的故事。',description:'发现同校的闲置好物，让校园生活多一点轻松与惊喜。',action:'发现校园好物',target:'market'},
  wanted:{eyebrow:'WANTED BOARD · 校园求购',title:'你正在寻找的，',highlight:'也许同学正好有。',description:'写下版本、成色与预算，让你的需求遇见合适的闲置。',action:'发布一条求购',target:'want'},
  mine:{eyebrow:'MY LITTLE SHOP · 我的发布',title:'整理一方小天地，',highlight:'给闲置一个新去处。',description:'认真描述商品、及时回复咨询，让每一次转让更顺利。',action:'发布我的闲置',target:'publish'},
  favorites:{eyebrow:'SAVED FOR LATER · 我的收藏',title:'把喜欢留在这里，',highlight:'慢慢选，也不怕错过。',description:'保存心动的好物，聊清细节后，再决定下一次校园面交。',action:'继续发现好物',target:'market'},
  orders:{eyebrow:'GOOD THINGS IN MOTION · 我的订单',title:'好物的下一站，',highlight:'从一次安心交接开始。',description:'查看购买意向与交易状态，约好时间、地点，再当面确认。',action:'与同学聊一聊',target:'messages'},
  messages:{eyebrow:'CAMPUS CONVERSATIONS · 校园消息',title:'先聊清楚，',highlight:'再把好物带回去。',description:'成色、配件、地点和时间，有问题就和同学聊一聊。',action:'查看我的咨询',target:'messages'}
}
const adminThemes={
  market:['浏览校园集市','看看校园好物，','了解同学们的需求。','浏览商品内容，发现需要审核和完善的信息。','返回管理后台','overview'],
  overview:['运营概览','让校园交易','有序地继续流动。','查看平台数据，及时跟进订单、求购与举报。','查看商品审核','goods'],
  goods:['商品审核','守护商品信息，','让每一次选择更放心。','核对成色与图片，及时处理不合适的商品。','查看商品记录','goods'],
  users:['用户管理','维护校园账号，','让交流更有秩序。','核实用户状态，维护公平、友好的交易环境。','查看用户记录','users'],
  orders:['订单监管','关注每次交接，','让交易状态清晰可查。','跟进购买意向，核实订单的取消与完成情况。','查看订单记录','orders'],
  messages:['留言管理','认真回应咨询，','让沟通更及时。','查看同学反馈，协助买卖双方聊清商品细节。','查看留言记录','messages'],
  wants:['求购审核','让合适的需求，','遇见合适的闲置。','审核求购内容，维护清晰、真实的校园需求。','查看求购记录','wants'],
  reports:['举报中心','及时核实反馈，','共同守护校园集市。','检查违规线索，跟进反馈的处理结果。','查看举报记录','reports']
}
const slides=computed(()=>{
  let entries
  if(props.admin){
    const theme=adminThemes[props.page]||adminThemes.overview
    entries=[
      {eyebrow:'ADMIN WORKSPACE · '+theme[0],title:theme[1],highlight:theme[2],description:theme[3],action:theme[4],target:theme[5]},
      {eyebrow:'CONTENT QUALITY · '+theme[0],title:'把好内容留下，',highlight:'把问题及时处理。',description:theme[3],action:'进入商品审核',target:'goods'},
      {eyebrow:'CAMPUS FEEDBACK · 校园反馈',title:'听见同学的反馈，',highlight:'让集市持续改善。',description:'关注留言与举报，及时处理需要跟进的问题。',action:'进入举报中心',target:'reports'}
    ]
  }else{
    const theme=themes[props.page]||themes.market
    entries=[
      {...theme},
      {eyebrow:'STUDY TOGETHER · '+theme.eyebrow.split(' · ')[1],title:'把用过的知识，',highlight:'交给下一位同学。',description:'教材、课程资料与生活好物，继续陪伴下一段校园时光。',action:'进入求购大厅',target:'wanted'},
      {eyebrow:'MEET ON CAMPUS · 安心面交',title:'先沟通，再见面，',highlight:'让好物交接更安心。',description:'约定校内公共地点，当面确认成色与配件，再完成交接。',action:'查看校园消息',target:'messages'}
    ]
  }
  return entries.map((slide,index)=>({...slide,image:'/images/banners/'+bannerKey.value+'-'+(index+1)+'.webp',alt:slide.eyebrow+' · 第 '+(index+1)+' 张主题照片'}))
})
function visibilityChanged(){hidden.value=document.hidden}
watch(()=>[props.page,props.admin],()=>{active.value=0;paused.value=false;carousel.value?.setActiveItem(0)},{flush:'post'})
onMounted(()=>{visibilityChanged();document.addEventListener('visibilitychange',visibilityChanged)})
onUnmounted(()=>{document.removeEventListener('visibilitychange',visibilityChanged)})
</script>

<style scoped>
.page-banner{position:relative;overflow:hidden;border-radius:20px;background:#dce7da;box-shadow:0 8px 24px #2946380b;border:1px solid #dce5d7}.banner-scene{position:relative;width:100%;height:100%}.banner-picture{width:100%;height:100%;object-fit:cover;object-position:center}.banner-shade{position:absolute;inset:0;background:linear-gradient(90deg,#153528ed 0%,#234835c7 35%,#284a3850 68%,#24422a12 100%)}.banner-copy{position:absolute;z-index:1;top:46px;left:60px;max-width:610px;color:#fff}.banner-eyebrow{font-size:11px;letter-spacing:1.7px;color:#e3eddb;margin:0 0 16px}.banner-copy h1,.banner-copy h2{font-size:42px;line-height:1.35;letter-spacing:-1px;margin:0 0 16px;font-weight:750}.banner-copy em{font-style:normal;color:#daeab4}.banner-description{font-size:13px;line-height:1.9;color:#f3f6e9;margin:0 0 22px;max-width:480px}.banner-action{background:#f4f8ea;color:#284c35;border-color:#e9f0db;height:40px;font-size:12px}.banner-action:hover{background:#fff}.banner-controls{position:absolute;left:60px;right:52px;bottom:20px;z-index:3;display:flex;align-items:center;gap:17px;color:#eef4e1}.banner-number{font-size:10px;letter-spacing:1px}.banner-dots{display:flex;align-items:center;gap:7px}.banner-dots button{padding:10px 0;width:23px;border:0;position:relative;background:none}.banner-dots button:after{content:"";display:block;height:4px;background:#ffffff70;border-radius:5px}.banner-dots button.active{width:40px}.banner-dots button.active:after{background:#d8eaaa}.banner-pause{margin-left:auto;display:flex;align-items:center;gap:5px;border:1px solid #ffffff55;border-radius:7px;padding:5px 9px;background:#ffffff14;color:#fff;font-size:10px}.banner-pause .el-icon{font-size:14px}.banner-compact .banner-copy{top:24px;left:48px;max-width:620px}.banner-compact .banner-copy h2{font-size:27px;line-height:1.35;margin:0 0 7px}.banner-compact .banner-eyebrow{font-size:9px;margin-bottom:10px}.banner-compact .banner-description{font-size:11px;margin-bottom:9px}.banner-compact .banner-action{height:31px;font-size:11px;padding:0 13px}.banner-compact .banner-controls{bottom:9px;left:48px}.page-banner :deep(.el-carousel__arrow){width:33px;height:33px;border:1px solid #ffffff50;background:#ffffff22}.page-banner :deep(.el-carousel__arrow:hover){background:#ffffff40}.page-banner button:focus-visible{outline:3px solid #f0d5a9;outline-offset:3px}
@media(max-width:1240px){.banner-copy h1{font-size:37px}.banner-copy{left:50px;max-width:560px}}
@media(prefers-reduced-motion:reduce){.page-banner :deep(.el-carousel__item){transition:none!important}}
@media(max-width:600px){.banner-copy,.banner-compact .banner-copy{left:24px;right:24px;top:30px;max-width:none}.banner-copy h1{font-size:31px}.banner-compact .banner-copy h2{font-size:23px}.banner-copy h1,.banner-copy h2{letter-spacing:0}.banner-description{font-size:12px;max-width:260px}.banner-controls,.banner-compact .banner-controls{left:24px;right:20px;gap:9px}.banner-eyebrow{font-size:9px}.page-banner{border-radius:14px}.page-banner :deep(.el-carousel__arrow){display:none}}
</style>
