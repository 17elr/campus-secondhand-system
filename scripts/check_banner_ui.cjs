const assert=require('node:assert/strict')
const {chromium}=require(process.argv[2]||'playwright')

async function login(page,username,password){
  await page.goto('http://127.0.0.1:15173',{waitUntil:'domcontentloaded'})
  await page.getByPlaceholder('输入用户名',{exact:true}).fill(username)
  await page.getByPlaceholder('输入密码',{exact:true}).fill(password)
  await page.getByRole('button',{name:'登录集市',exact:true}).click()
  await page.getByRole('dialog',{name:'欢迎回到拾光集市'}).waitFor({state:'hidden'})
}

async function waitForChange(page,previous){
  await page.waitForFunction(image=>document.querySelector('.banner-scene[aria-hidden="false"] img')?.getAttribute('src')!==image,previous,{timeout:7000})
  await page.waitForFunction(()=>document.querySelector('.banner-scene[aria-hidden="false"] img')?.naturalWidth>0)
}

async function main(){
  const browser=await chromium.launch({channel:'msedge',headless:true})
  const images=new Set(),errors=[],checks=[]
  async function verify(page,key){
    await page.locator('.page-banner').waitFor()
    const paths=await page.locator('.banner-scene img').evaluateAll(items=>items.map(item=>item.getAttribute('src')))
    assert.equal(paths.length,3,key)
    for(const path of paths){
      assert.ok(path.startsWith('/images/banners/'+key+'-'),key+' uses another page image')
      assert.ok(!images.has(path),path+' is reused')
      images.add(path)
    }
    await page.locator('.page-banner').hover()
    const previous=await page.locator('.banner-scene[aria-hidden="false"] img').getAttribute('src')
    await waitForChange(page,previous)
    checks.push(key)
    console.log('autoplay while hovered:',key)
  }
  try{
    const user=await browser.newPage({viewport:{width:1440,height:1050}})
    user.on('pageerror',error=>errors.push(error.message))
    await login(user,'student','123456')
    await user.locator('.goods-card').first().waitFor()
    await verify(user,'market')
    await user.locator('.el-carousel__arrow--right').click()
    const focused=await user.locator('.banner-scene[aria-hidden="false"] img').getAttribute('src')
    await waitForChange(user,focused)
    console.log('autoplay after arrow click: passed')
    await user.getByRole('button',{name:'暂停轮播',exact:true}).click()
    const stopped=await user.locator('.banner-scene[aria-hidden="false"] img').getAttribute('src')
    await user.waitForTimeout(4200)
    assert.equal(await user.locator('.banner-scene[aria-hidden="false"] img').getAttribute('src'),stopped)
    await user.getByRole('button',{name:'播放轮播',exact:true}).click()
    await waitForChange(user,stopped)
    await user.getByRole('button',{name:'暂停轮播',exact:true}).click()
    for(const [label,key] of [['求购大厅','wanted'],['我的发布','mine'],['收藏','favorites'],['订单','orders'],['消息','messages']]){
      await user.locator('.main-nav').getByRole('button',{name:label,exact:true}).click()
      await user.waitForFunction(()=>document.querySelector('.market-section')?.getAttribute('aria-busy')==='false')
      await verify(user,key)
      assert.ok(await user.getByRole('button',{name:'暂停轮播',exact:true}).isVisible())
    }
    const admin=await browser.newPage({viewport:{width:1440,height:1050},reducedMotion:'reduce'})
    admin.on('pageerror',error=>errors.push(error.message))
    await login(admin,'admin','admin123')
    await admin.locator('.recent-item').first().waitFor()
    for(const [label,key] of [['运营概览','overview'],['商品审核','goods'],['用户管理','users'],['订单监管','orders'],['留言管理','messages'],['求购审核','wants'],['举报中心','reports']]){
      await admin.locator('.admin-rail').getByRole('button',{name:label,exact:false}).click()
      await verify(admin,'admin-'+key)
    }
    await admin.locator('.main-nav').getByRole('button',{name:'浏览集市',exact:true}).click()
    await admin.locator('.goods-card').first().waitFor()
    await verify(admin,'admin-market')
    assert.equal(images.size,42)
    const decoded=await admin.evaluate(async urls=>Promise.all(urls.map(url=>new Promise(resolve=>{const image=new Image();image.onload=()=>resolve(image.naturalWidth);image.onerror=()=>resolve(0);image.src=url}))),[...images])
    assert.ok(decoded.every(width=>width===1600))
    assert.deepEqual(errors,[])
    console.log(JSON.stringify({pages:checks.length,uniqueImages:images.size,hoverAutoplay:true,focusedAutoplay:true,pauseAndResume:true,pagePauseReset:true,reducedMotionAutoplay:true,pageErrors:errors}))
  }finally{await browser.close()}
}

main().catch(error=>{console.error(error);process.exit(1)})
