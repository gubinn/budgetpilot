import{s as O}from"./index-B8UIWwSO.js";import"./dayjs.min-BAJkae7J.js";import{a as ne,b as L,o as k,g as r,w as re,i as V,n as W,be as le,bf as ae,d as Z,bn as se,h as m,b5 as ie,u as de,j as ee,l as ce,m as H,a9 as U,K as pe,L as E,M as t,r as N,H as z,R as n,_ as ue,U as be,$ as P,I as K,F as Y,Y as J,J as X,X as G,T as q}from"./index-CmO3RiWV.js";import{c as ge,a as me}from"./get-DwjtG2nT.js";import{u as _e,a as he,_ as fe}from"./request-Bm_zkcpX.js";import{_ as ve,a as ye}from"./Grid-Cp0UAkRl.js";import{_ as xe,a as Ce}from"./FormItem-CTQEZNCo.js";import{_ as we}from"./Input-G4L66MPZ.js";import{_ as Se}from"./DynamicTags-BpsV0KmT.js";import{_ as ke,a as ze,b as Pe}from"./Thing-jiGSHAln.js";import"./Suffix-CKQRF8b6.js";import"./Add-CE5SX8Ex.js";import"./Space-DLKVJyiU.js";import"./prop-NnGblK-3.js";function Q(i,b="default",s=[]){const{children:c}=i;if(c!==null&&typeof c=="object"&&!Array.isArray(c)){const p=c[b];if(typeof p=="function")return p()}return s}const Re={thPaddingBorderedSmall:"8px 12px",thPaddingBorderedMedium:"12px 16px",thPaddingBorderedLarge:"16px 24px",thPaddingSmall:"0",thPaddingMedium:"0",thPaddingLarge:"0",tdPaddingBorderedSmall:"8px 12px",tdPaddingBorderedMedium:"12px 16px",tdPaddingBorderedLarge:"16px 24px",tdPaddingSmall:"0 0 8px 0",tdPaddingMedium:"0 0 12px 0",tdPaddingLarge:"0 0 16px 0"};function $e(i){const{tableHeaderColor:b,textColor2:s,textColor1:c,cardColor:p,modalColor:_,popoverColor:v,dividerColor:h,borderRadius:u,fontWeightStrong:g,lineHeight:M,fontSizeSmall:l,fontSizeMedium:y,fontSizeLarge:C}=i;return Object.assign(Object.assign({},Re),{lineHeight:M,fontSizeSmall:l,fontSizeMedium:y,fontSizeLarge:C,titleTextColor:c,thColor:L(p,b),thColorModal:L(_,b),thColorPopover:L(v,b),thTextColor:c,thFontWeight:g,tdTextColor:s,tdColor:p,tdColorModal:_,tdColorPopover:v,borderColor:L(p,h),borderColorModal:L(_,h),borderColorPopover:L(v,h),borderRadius:u})}const Be={common:ne,self:$e},Te=k([r("descriptions",{fontSize:"var(--n-font-size)"},[r("descriptions-separator",`
 display: inline-block;
 margin: 0 8px 0 2px;
 `),r("descriptions-table-wrapper",[r("descriptions-table",[r("descriptions-table-row",[r("descriptions-table-header",{padding:"var(--n-th-padding)"}),r("descriptions-table-content",{padding:"var(--n-td-padding)"})])])]),re("bordered",[r("descriptions-table-wrapper",[r("descriptions-table",[r("descriptions-table-row",[k("&:last-child",[r("descriptions-table-content",{paddingBottom:0})])])])])]),V("left-label-placement",[r("descriptions-table-content",[k("> *",{verticalAlign:"top"})])]),V("left-label-align",[k("th",{textAlign:"left"})]),V("center-label-align",[k("th",{textAlign:"center"})]),V("right-label-align",[k("th",{textAlign:"right"})]),V("bordered",[r("descriptions-table-wrapper",`
 border-radius: var(--n-border-radius);
 overflow: hidden;
 background: var(--n-merged-td-color);
 border: 1px solid var(--n-merged-border-color);
 `,[r("descriptions-table",[r("descriptions-table-row",[k("&:not(:last-child)",[r("descriptions-table-content",{borderBottom:"1px solid var(--n-merged-border-color)"}),r("descriptions-table-header",{borderBottom:"1px solid var(--n-merged-border-color)"})]),r("descriptions-table-header",`
 font-weight: 400;
 background-clip: padding-box;
 background-color: var(--n-merged-th-color);
 `,[k("&:not(:last-child)",{borderRight:"1px solid var(--n-merged-border-color)"})]),r("descriptions-table-content",[k("&:not(:last-child)",{borderRight:"1px solid var(--n-merged-border-color)"})])])])])]),r("descriptions-header",`
 font-weight: var(--n-th-font-weight);
 font-size: 18px;
 transition: color .3s var(--n-bezier);
 line-height: var(--n-line-height);
 margin-bottom: 16px;
 color: var(--n-title-text-color);
 `),r("descriptions-table-wrapper",`
 transition:
 background-color .3s var(--n-bezier),
 border-color .3s var(--n-bezier);
 `,[r("descriptions-table",`
 width: 100%;
 border-collapse: separate;
 border-spacing: 0;
 box-sizing: border-box;
 `,[r("descriptions-table-row",`
 box-sizing: border-box;
 transition: border-color .3s var(--n-bezier);
 `,[r("descriptions-table-header",`
 font-weight: var(--n-th-font-weight);
 line-height: var(--n-line-height);
 display: table-cell;
 box-sizing: border-box;
 color: var(--n-th-text-color);
 transition:
 color .3s var(--n-bezier),
 background-color .3s var(--n-bezier),
 border-color .3s var(--n-bezier);
 `),r("descriptions-table-content",`
 vertical-align: top;
 line-height: var(--n-line-height);
 display: table-cell;
 box-sizing: border-box;
 color: var(--n-td-text-color);
 transition:
 color .3s var(--n-bezier),
 background-color .3s var(--n-bezier),
 border-color .3s var(--n-bezier);
 `,[W("content",`
 transition: color .3s var(--n-bezier);
 display: inline-block;
 color: var(--n-td-text-color);
 `)]),W("label",`
 font-weight: var(--n-th-font-weight);
 transition: color .3s var(--n-bezier);
 display: inline-block;
 margin-right: 14px;
 color: var(--n-th-text-color);
 `)])])])]),r("descriptions-table-wrapper",`
 --n-merged-th-color: var(--n-th-color);
 --n-merged-td-color: var(--n-td-color);
 --n-merged-border-color: var(--n-border-color);
 `),le(r("descriptions-table-wrapper",`
 --n-merged-th-color: var(--n-th-color-modal);
 --n-merged-td-color: var(--n-td-color-modal);
 --n-merged-border-color: var(--n-border-color-modal);
 `)),ae(r("descriptions-table-wrapper",`
 --n-merged-th-color: var(--n-th-color-popover);
 --n-merged-td-color: var(--n-td-color-popover);
 --n-merged-border-color: var(--n-border-color-popover);
 `))]),te="DESCRIPTION_ITEM_FLAG";function Me(i){return typeof i=="object"&&i&&!Array.isArray(i)?i.type&&i.type[te]:!1}const Ae=Object.assign(Object.assign({},ee.props),{title:String,column:{type:Number,default:3},columns:Number,labelPlacement:{type:String,default:"top"},labelAlign:{type:String,default:"left"},separator:{type:String,default:":"},size:String,bordered:Boolean,labelClass:String,labelStyle:[Object,String],contentClass:String,contentStyle:[Object,String]}),Ie=Z({name:"Descriptions",props:Ae,slots:Object,setup(i){const{mergedClsPrefixRef:b,inlineThemeDisabled:s,mergedComponentPropsRef:c}=de(i),p=H(()=>{var u,g;return i.size||((g=(u=c==null?void 0:c.value)===null||u===void 0?void 0:u.Descriptions)===null||g===void 0?void 0:g.size)||"medium"}),_=ee("Descriptions","-descriptions",Te,Be,i,b),v=H(()=>{const{bordered:u}=i,g=p.value,{common:{cubicBezierEaseInOut:M},self:{titleTextColor:l,thColor:y,thColorModal:C,thColorPopover:a,thTextColor:e,thFontWeight:w,tdTextColor:x,tdColor:o,tdColorModal:R,tdColorPopover:$,borderColor:f,borderColorModal:B,borderColorPopover:S,borderRadius:A,lineHeight:T,[U("fontSize",g)]:I,[U(u?"thPaddingBordered":"thPadding",g)]:j,[U(u?"tdPaddingBordered":"tdPadding",g)]:D}}=_.value;return{"--n-title-text-color":l,"--n-th-padding":j,"--n-td-padding":D,"--n-font-size":I,"--n-bezier":M,"--n-th-font-weight":w,"--n-line-height":T,"--n-th-text-color":e,"--n-td-text-color":x,"--n-th-color":y,"--n-th-color-modal":C,"--n-th-color-popover":a,"--n-td-color":o,"--n-td-color-modal":R,"--n-td-color-popover":$,"--n-border-radius":A,"--n-border-color":f,"--n-border-color-modal":B,"--n-border-color-popover":S}}),h=s?ce("descriptions",H(()=>{let u="";const{bordered:g}=i;return g&&(u+="a"),u+=p.value[0],u}),v,i):void 0;return{mergedClsPrefix:b,cssVars:s?void 0:v,themeClass:h==null?void 0:h.themeClass,onRender:h==null?void 0:h.onRender,compitableColumn:me(i,["columns","column"]),inlineThemeDisabled:s,mergedSize:p}},render(){const i=this.$slots.default,b=i?se(i()):[];b.length;const{contentClass:s,labelClass:c,compitableColumn:p,labelPlacement:_,labelAlign:v,mergedSize:h,bordered:u,title:g,cssVars:M,mergedClsPrefix:l,separator:y,onRender:C}=this;C==null||C();const a=b.filter(o=>Me(o)),e={span:0,row:[],secondRow:[],rows:[]},x=a.reduce((o,R,$)=>{const f=R.props||{},B=a.length-1===$,S=["label"in f?f.label:Q(R,"label")],A=[Q(R)],T=f.span||1,I=o.span;o.span+=T;const j=f.labelStyle||f["label-style"]||this.labelStyle,D=f.contentStyle||f["content-style"]||this.contentStyle;if(_==="left")u?o.row.push(m("th",{class:[`${l}-descriptions-table-header`,c],colspan:1,style:j},S),m("td",{class:[`${l}-descriptions-table-content`,s],colspan:B?(p-I)*2+1:T*2-1,style:D},A)):o.row.push(m("td",{class:`${l}-descriptions-table-content`,colspan:B?(p-I)*2:T*2},m("span",{class:[`${l}-descriptions-table-content__label`,c],style:j},[...S,y&&m("span",{class:`${l}-descriptions-separator`},y)]),m("span",{class:[`${l}-descriptions-table-content__content`,s],style:D},A)));else{const F=B?(p-I)*2:T*2;o.row.push(m("th",{class:[`${l}-descriptions-table-header`,c],colspan:F,style:j},S)),o.secondRow.push(m("td",{class:[`${l}-descriptions-table-content`,s],colspan:F,style:D},A))}return(o.span>=p||B)&&(o.span=0,o.row.length&&(o.rows.push(o.row),o.row=[]),_!=="left"&&o.secondRow.length&&(o.rows.push(o.secondRow),o.secondRow=[])),o},e).rows.map(o=>m("tr",{class:`${l}-descriptions-table-row`},o));return m("div",{style:M,class:[`${l}-descriptions`,this.themeClass,`${l}-descriptions--${_}-label-placement`,`${l}-descriptions--${v}-label-align`,`${l}-descriptions--${h}-size`,u&&`${l}-descriptions--bordered`]},g||this.$slots.header?m("div",{class:`${l}-descriptions-header`},g||ge(this,"header")):null,m("div",{class:`${l}-descriptions-table-wrapper`},m("table",{class:`${l}-descriptions-table`},m("tbody",null,_==="top"&&m("tr",{class:`${l}-descriptions-table-row`,style:{visibility:"collapse"}},ie(p*2,m("td",null))),x))))}}),je={label:String,span:{type:Number,default:1},labelClass:String,labelStyle:[Object,String],contentClass:String,contentStyle:[Object,String]},De=Z({name:"DescriptionsItem",[te]:!0,props:je,slots:Object,render(){return null}}),Qe={__name:"SettingsPage",setup(i){const b=_e(),s=N({telegram_bot_token:"",telegram_chat_id:"",exchange_rate_api_key:""}),c=N([]),p=N([]),_=N([]),v=N(!1);async function h(){try{const a=await O.config();s.value.telegram_bot_token=a.data.telegram_bot_token||"",s.value.telegram_chat_id=a.data.telegram_chat_id||"",s.value.exchange_rate_api_key=a.data.exchange_rate_api_key||"";const e=a.data.supported_currencies||"CNY,USD,EUR,GBP,JPY,HKD,SGD,THB,KRW";c.value=e.split(",").map(w=>w.trim()).filter(Boolean)}catch{}}async function u(a,e){try{await O.setConfig(a,e),b.success("保存成功")}catch{b.error("保存失败")}}async function g(){try{const a=await O.testTelegram();b.success(a.message||"推送成功")}catch{b.error("推送失败")}}async function M(){v.value=!0;try{await O.refreshRates(),b.success("刷新成功"),l()}catch{b.error("刷新失败")}finally{v.value=!1}}async function l(){try{const a=await O.rates();p.value=a.data||[]}catch{}}async function y(){try{const a=await O.alerts();_.value=a.data||[]}catch{}}async function C(a){try{await O.markAlertRead(a),y()}catch{}}return pe(()=>{h(),l(),y()}),(a,e)=>{const w=we,x=Ce,o=be,R=xe,$=ue,f=ye,B=Se,S=De,A=Ie,T=he,I=Pe,j=ze,D=ke,F=fe,oe=ve;return z(),E(oe,{cols:2,"x-gap":16,"y-gap":16},{default:t(()=>[n(f,null,{default:t(()=>[n($,{title:"Telegram 通知",hoverable:""},{default:t(()=>[n(R,{"label-placement":"left","label-width":"80"},{default:t(()=>[n(x,{label:"Bot Token"},{default:t(()=>[n(w,{value:s.value.telegram_bot_token,"onUpdate:value":e[0]||(e[0]=d=>s.value.telegram_bot_token=d),type:"password","show-password-on":"click",placeholder:"输入 Bot Token"},null,8,["value"])]),_:1}),n(x,{label:"Chat ID"},{default:t(()=>[n(w,{value:s.value.telegram_chat_id,"onUpdate:value":e[1]||(e[1]=d=>s.value.telegram_chat_id=d),placeholder:"输入 Chat ID"},null,8,["value"])]),_:1}),n(x,null,{default:t(()=>[n(o,{type:"primary",onClick:e[2]||(e[2]=d=>{u("telegram_bot_token",s.value.telegram_bot_token),u("telegram_chat_id",s.value.telegram_chat_id)})},{default:t(()=>[...e[6]||(e[6]=[P(" 保存配置 ",-1)])]),_:1}),n(o,{onClick:g,style:{"margin-left":"8px"}},{default:t(()=>[...e[7]||(e[7]=[P("推送测试",-1)])]),_:1})]),_:1})]),_:1})]),_:1})]),_:1}),n(f,null,{default:t(()=>[n($,{title:"汇率设置",hoverable:""},{default:t(()=>[n(R,{"label-placement":"left","label-width":"80"},{default:t(()=>[n(x,{label:"API Key"},{default:t(()=>[n(w,{value:s.value.exchange_rate_api_key,"onUpdate:value":e[3]||(e[3]=d=>s.value.exchange_rate_api_key=d),type:"password","show-password-on":"click",placeholder:"ExchangeRate-API Key"},null,8,["value"])]),_:1}),n(x,{label:"支持的币种"},{default:t(()=>[n(B,{value:c.value,"onUpdate:value":e[4]||(e[4]=d=>c.value=d)},null,8,["value"])]),_:1}),n(x,null,{default:t(()=>[n(o,{type:"primary",onClick:e[5]||(e[5]=d=>{u("exchange_rate_api_key",s.value.exchange_rate_api_key),u("supported_currencies",c.value.join(","))})},{default:t(()=>[...e[8]||(e[8]=[P(" 保存配置 ",-1)])]),_:1}),n(o,{onClick:M,loading:v.value,style:{"margin-left":"8px"}},{default:t(()=>[...e[9]||(e[9]=[P("手动刷新",-1)])]),_:1},8,["loading"])]),_:1})]),_:1})]),_:1})]),_:1}),n(f,null,{default:t(()=>[n($,{title:"系统信息",hoverable:""},{default:t(()=>[n(A,{bordered:"",column:1,size:"small"},{default:t(()=>[n(S,{label:"本位币"},{default:t(()=>[...e[10]||(e[10]=[P("CNY",-1)])]),_:1}),n(S,{label:"月度起始日"},{default:t(()=>[...e[11]||(e[11]=[P("每月 1 日",-1)])]),_:1}),p.value.length>0?(z(),E(S,{key:0,label:"当前汇率"},{default:t(()=>[(z(!0),K(Y,null,J(p.value.slice(0,5),d=>(z(),K("div",{key:d.targetCurrency,style:{display:"flex","justify-content":"space-between",padding:"4px 0"}},[X("span",null,"1 "+G(d.targetCurrency),1),X("span",null,"¥ "+G(Number(d.rate).toFixed(4)),1)]))),128))]),_:1})):q("",!0)]),_:1})]),_:1})]),_:1}),n(f,null,{default:t(()=>[n($,{title:"预警日志",hoverable:""},{"header-extra":t(()=>[n(o,{size:"small",onClick:y},{default:t(()=>[...e[12]||(e[12]=[P("刷新",-1)])]),_:1})]),default:t(()=>[_.value.length>0?(z(),E(D,{key:0},{default:t(()=>[(z(!0),K(Y,null,J(_.value,d=>(z(),E(j,{key:d.id},{prefix:t(()=>[n(T,{type:d.isSent?"success":"error",size:"small"},{default:t(()=>[P(G(d.isSent?"已推送":"未推送"),1)]),_:2},1032,["type"])]),suffix:t(()=>[d.isRead?q("",!0):(z(),E(o,{key:0,size:"tiny",text:"",onClick:Oe=>C(d.id)},{default:t(()=>[...e[13]||(e[13]=[P("标记已读",-1)])]),_:1},8,["onClick"]))]),default:t(()=>[n(I,{title:d.title,description:d.content},null,8,["title","description"])]),_:2},1024))),128))]),_:1})):(z(),E(F,{key:1,description:"暂无预警"}))]),_:1})]),_:1})]),_:1})}}};export{Qe as default};
