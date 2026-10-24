let csrfToken='', csrfRequest
async function csrf(){
 if(csrfToken)return csrfToken
 if(!csrfRequest)csrfRequest=fetch('/api/auth/csrf',{credentials:'same-origin'}).then(async response=>{if(!response.ok)throw new Error('登录服务暂不可用');csrfToken=(await response.json()).data.token;return csrfToken}).finally(()=>{csrfRequest=null})
 return csrfRequest
}
async function send(method,url,body,options={}){
 const query=new URLSearchParams(Object.entries(options.params||{}).filter(([,v])=>v!==undefined&&v!==null))
 const headers={...options.headers}
 if(!['GET','HEAD'].includes(method))headers['X-CSRF-Token']=await csrf()
 const form=body instanceof FormData
 if(body!=null&&!form)headers['Content-Type']='application/json'
 let response
 try{response=await fetch(url+(query.size?'?'+query:''),{method,headers,credentials:'same-origin',body:body==null?undefined:form?body:JSON.stringify(body),signal:options.signal||AbortSignal.timeout(options.timeout||15000)})}
 catch(cause){const error=new Error('网络连接中断，请重试');error.isApiError=true;error.cause=cause;throw error}
 const data=response.status===204?{data:null}:await response.json().catch(()=>({message:'服务响应异常'}))
 if(!response.ok){const error=new Error(data.message||data.detail||'请求未完成');error.response={status:response.status,data};error.isApiError=true;if(response.status===401&&!url.endsWith('/login')&&!url.endsWith('/me'))window.dispatchEvent(new Event('session-expired'));throw error}
 if(['/api/auth/login','/api/auth/register','/api/auth/logout','/api/auth/password'].includes(url))csrfToken=''
 return {data}
}
export const api={get:(url,options)=>send('GET',url,null,options),post:(url,body,options)=>send('POST',url,body,options),put:(url,body,options)=>send('PUT',url,body,options),patch:(url,body,options)=>send('PATCH',url,body,options),delete:(url,options)=>send('DELETE',url,null,options)}
