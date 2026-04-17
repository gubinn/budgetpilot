import{s as H}from"./index-DdZDHS62.js";import"./dayjs.min-BAJkae7J.js";import{j as Z,D as R,a as x,e as r,L as ce,g as B,i as y,bk as ne,bl as le,d as W,bt as pe,h as n,ba as ue,p as Q,q as F,x as X,c as K,z as G,v as ie,F as ge,H as be,t as me,J as he,b8 as ve,R as q,s as fe,a0 as V,a1 as s,r as N,Z as T,a5 as g,aa as _e,a7 as xe,ad as E,_ as Y,ab as ee,$ as te,a9 as J,a6 as oe}from"./index-B7k3i8Dy.js";import{c as ye,a as Ce}from"./get-DX8-5hPx.js";import{u as we,b as Se,_ as ze}from"./request-DW35_YEC.js";import{_ as $e,a as ke}from"./Grid-B7RiT_28.js";import{_ as Pe,a as Re}from"./FormItem-CP22HhXG.js";import{_ as Be}from"./Input-ChmBkpNM.js";import{_ as je}from"./DynamicTags-DOD_-ezt.js";import"./Add-Dx_2bxqe.js";import"./Space-BFV8xjOh.js";import"./prop-NnGblK-3.js";function re(e,t="default",o=[]){const{children:i}=e;if(i!==null&&typeof i=="object"&&!Array.isArray(i)){const p=i[t];if(typeof p=="function")return p()}return o}const Te={thPaddingBorderedSmall:"8px 12px",thPaddingBorderedMedium:"12px 16px",thPaddingBorderedLarge:"16px 24px",thPaddingSmall:"0",thPaddingMedium:"0",thPaddingLarge:"0",tdPaddingBorderedSmall:"8px 12px",tdPaddingBorderedMedium:"12px 16px",tdPaddingBorderedLarge:"16px 24px",tdPaddingSmall:"0 0 8px 0",tdPaddingMedium:"0 0 12px 0",tdPaddingLarge:"0 0 16px 0"};function Ee(e){const{tableHeaderColor:t,textColor2:o,textColor1:i,cardColor:p,modalColor:v,popoverColor:_,dividerColor:m,borderRadius:a,fontWeightStrong:h,lineHeight:u,fontSizeSmall:c,fontSizeMedium:C,fontSizeLarge:S}=e;return Object.assign(Object.assign({},Te),{lineHeight:u,fontSizeSmall:c,fontSizeMedium:C,fontSizeLarge:S,titleTextColor:i,thColor:R(p,t),thColorModal:R(v,t),thColorPopover:R(_,t),thTextColor:i,thFontWeight:h,tdTextColor:o,tdColor:p,tdColorModal:v,tdColorPopover:_,borderColor:R(p,m),borderColorModal:R(v,m),borderColorPopover:R(_,m),borderRadius:a})}const De={common:Z,self:Ee},Ie=x([r("descriptions",{fontSize:"var(--n-font-size)"},[r("descriptions-separator",`
 display: inline-block;
 margin: 0 8px 0 2px;
 `),r("descriptions-table-wrapper",[r("descriptions-table",[r("descriptions-table-row",[r("descriptions-table-header",{padding:"var(--n-th-padding)"}),r("descriptions-table-content",{padding:"var(--n-td-padding)"})])])]),ce("bordered",[r("descriptions-table-wrapper",[r("descriptions-table",[r("descriptions-table-row",[x("&:last-child",[r("descriptions-table-content",{paddingBottom:0})])])])])]),B("left-label-placement",[r("descriptions-table-content",[x("> *",{verticalAlign:"top"})])]),B("left-label-align",[x("th",{textAlign:"left"})]),B("center-label-align",[x("th",{textAlign:"center"})]),B("right-label-align",[x("th",{textAlign:"right"})]),B("bordered",[r("descriptions-table-wrapper",`
 border-radius: var(--n-border-radius);
 overflow: hidden;
 background: var(--n-merged-td-color);
 border: 1px solid var(--n-merged-border-color);
 `,[r("descriptions-table",[r("descriptions-table-row",[x("&:not(:last-child)",[r("descriptions-table-content",{borderBottom:"1px solid var(--n-merged-border-color)"}),r("descriptions-table-header",{borderBottom:"1px solid var(--n-merged-border-color)"})]),r("descriptions-table-header",`
 font-weight: 400;
 background-clip: padding-box;
 background-color: var(--n-merged-th-color);
 `,[x("&:not(:last-child)",{borderRight:"1px solid var(--n-merged-border-color)"})]),r("descriptions-table-content",[x("&:not(:last-child)",{borderRight:"1px solid var(--n-merged-border-color)"})])])])])]),r("descriptions-header",`
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
 `,[y("content",`
 transition: color .3s var(--n-bezier);
 display: inline-block;
 color: var(--n-td-text-color);
 `)]),y("label",`
 font-weight: var(--n-th-font-weight);
 transition: color .3s var(--n-bezier);
 display: inline-block;
 margin-right: 14px;
 color: var(--n-th-text-color);
 `)])])])]),r("descriptions-table-wrapper",`
 --n-merged-th-color: var(--n-th-color);
 --n-merged-td-color: var(--n-td-color);
 --n-merged-border-color: var(--n-border-color);
 `),ne(r("descriptions-table-wrapper",`
 --n-merged-th-color: var(--n-th-color-modal);
 --n-merged-td-color: var(--n-td-color-modal);
 --n-merged-border-color: var(--n-border-color-modal);
 `)),le(r("descriptions-table-wrapper",`
 --n-merged-th-color: var(--n-th-color-popover);
 --n-merged-td-color: var(--n-td-color-popover);
 --n-merged-border-color: var(--n-border-color-popover);
 `))]),ae="DESCRIPTION_ITEM_FLAG";function Me(e){return typeof e=="object"&&e&&!Array.isArray(e)?e.type&&e.type[ae]:!1}const Oe=Object.assign(Object.assign({},F.props),{title:String,column:{type:Number,default:3},columns:Number,labelPlacement:{type:String,default:"top"},labelAlign:{type:String,default:"left"},separator:{type:String,default:":"},size:String,bordered:Boolean,labelClass:String,labelStyle:[Object,String],contentClass:String,contentStyle:[Object,String]}),Le=W({name:"Descriptions",props:Oe,slots:Object,setup(e){const{mergedClsPrefixRef:t,inlineThemeDisabled:o,mergedComponentPropsRef:i}=Q(e),p=K(()=>{var a,h;return e.size||((h=(a=i==null?void 0:i.value)===null||a===void 0?void 0:a.Descriptions)===null||h===void 0?void 0:h.size)||"medium"}),v=F("Descriptions","-descriptions",Ie,De,e,t),_=K(()=>{const{bordered:a}=e,h=p.value,{common:{cubicBezierEaseInOut:u},self:{titleTextColor:c,thColor:C,thColorModal:S,thColorPopover:b,thTextColor:l,thFontWeight:$,tdTextColor:z,tdColor:d,tdColorModal:k,tdColorPopover:P,borderColor:w,borderColorModal:D,borderColorPopover:j,borderRadius:M,lineHeight:I,[G("fontSize",h)]:O,[G(a?"thPaddingBordered":"thPadding",h)]:L,[G(a?"tdPaddingBordered":"tdPadding",h)]:A}}=v.value;return{"--n-title-text-color":c,"--n-th-padding":L,"--n-td-padding":A,"--n-font-size":O,"--n-bezier":u,"--n-th-font-weight":$,"--n-line-height":I,"--n-th-text-color":l,"--n-td-text-color":z,"--n-th-color":C,"--n-th-color-modal":S,"--n-th-color-popover":b,"--n-td-color":d,"--n-td-color-modal":k,"--n-td-color-popover":P,"--n-border-radius":M,"--n-border-color":w,"--n-border-color-modal":D,"--n-border-color-popover":j}}),m=o?X("descriptions",K(()=>{let a="";const{bordered:h}=e;return h&&(a+="a"),a+=p.value[0],a}),_,e):void 0;return{mergedClsPrefix:t,cssVars:o?void 0:_,themeClass:m==null?void 0:m.themeClass,onRender:m==null?void 0:m.onRender,compitableColumn:Ce(e,["columns","column"]),inlineThemeDisabled:o,mergedSize:p}},render(){const e=this.$slots.default,t=e?pe(e()):[];t.length;const{contentClass:o,labelClass:i,compitableColumn:p,labelPlacement:v,labelAlign:_,mergedSize:m,bordered:a,title:h,cssVars:u,mergedClsPrefix:c,separator:C,onRender:S}=this;S==null||S();const b=t.filter(d=>Me(d)),l={span:0,row:[],secondRow:[],rows:[]},z=b.reduce((d,k,P)=>{const w=k.props||{},D=b.length-1===P,j=["label"in w?w.label:re(k,"label")],M=[re(k)],I=w.span||1,O=d.span;d.span+=I;const L=w.labelStyle||w["label-style"]||this.labelStyle,A=w.contentStyle||w["content-style"]||this.contentStyle;if(v==="left")a?d.row.push(n("th",{class:[`${c}-descriptions-table-header`,i],colspan:1,style:L},j),n("td",{class:[`${c}-descriptions-table-content`,o],colspan:D?(p-O)*2+1:I*2-1,style:A},M)):d.row.push(n("td",{class:`${c}-descriptions-table-content`,colspan:D?(p-O)*2:I*2},n("span",{class:[`${c}-descriptions-table-content__label`,i],style:L},[...j,C&&n("span",{class:`${c}-descriptions-separator`},C)]),n("span",{class:[`${c}-descriptions-table-content__content`,o],style:A},M)));else{const U=D?(p-O)*2:I*2;d.row.push(n("th",{class:[`${c}-descriptions-table-header`,i],colspan:U,style:L},j)),d.secondRow.push(n("td",{class:[`${c}-descriptions-table-content`,o],colspan:U,style:A},M))}return(d.span>=p||D)&&(d.span=0,d.row.length&&(d.rows.push(d.row),d.row=[]),v!=="left"&&d.secondRow.length&&(d.rows.push(d.secondRow),d.secondRow=[])),d},l).rows.map(d=>n("tr",{class:`${c}-descriptions-table-row`},d));return n("div",{style:u,class:[`${c}-descriptions`,this.themeClass,`${c}-descriptions--${v}-label-placement`,`${c}-descriptions--${_}-label-align`,`${c}-descriptions--${m}-size`,a&&`${c}-descriptions--bordered`]},h||this.$slots.header?n("div",{class:`${c}-descriptions-header`},h||ye(this,"header")):null,n("div",{class:`${c}-descriptions-table-wrapper`},n("table",{class:`${c}-descriptions-table`},n("tbody",null,v==="top"&&n("tr",{class:`${c}-descriptions-table-row`,style:{visibility:"collapse"}},ue(p*2,n("td",null))),z))))}}),Ae={label:String,span:{type:Number,default:1},labelClass:String,labelStyle:[Object,String],contentClass:String,contentStyle:[Object,String]},He=W({name:"DescriptionsItem",[ae]:!0,props:Ae,slots:Object,render(){return null}});function Ve(e){const{textColor2:t,cardColor:o,modalColor:i,popoverColor:p,dividerColor:v,borderRadius:_,fontSize:m,hoverColor:a}=e;return{textColor:t,color:o,colorHover:a,colorModal:i,colorHoverModal:R(i,a),colorPopover:p,colorHoverPopover:R(p,a),borderColor:v,borderColorModal:R(i,v),borderColorPopover:R(p,v),borderRadius:_,fontSize:m}}const Fe={common:Z,self:Ve};function Ne(e){const{textColor1:t,textColor2:o,fontWeightStrong:i,fontSize:p}=e;return{fontSize:p,titleTextColor:t,textColor:o,titleFontWeight:i}}const Ke={common:Z,self:Ne},We=x([r("list",`
 --n-merged-border-color: var(--n-border-color);
 --n-merged-color: var(--n-color);
 --n-merged-color-hover: var(--n-color-hover);
 margin: 0;
 font-size: var(--n-font-size);
 transition:
 background-color .3s var(--n-bezier),
 color .3s var(--n-bezier),
 border-color .3s var(--n-bezier);
 padding: 0;
 list-style-type: none;
 color: var(--n-text-color);
 background-color: var(--n-merged-color);
 `,[B("show-divider",[r("list-item",[x("&:not(:last-child)",[y("divider",`
 background-color: var(--n-merged-border-color);
 `)])])]),B("clickable",[r("list-item",`
 cursor: pointer;
 `)]),B("bordered",`
 border: 1px solid var(--n-merged-border-color);
 border-radius: var(--n-border-radius);
 `),B("hoverable",[r("list-item",`
 border-radius: var(--n-border-radius);
 `,[x("&:hover",`
 background-color: var(--n-merged-color-hover);
 `,[y("divider",`
 background-color: transparent;
 `)])])]),B("bordered, hoverable",[r("list-item",`
 padding: 12px 20px;
 `),y("header, footer",`
 padding: 12px 20px;
 `)]),y("header, footer",`
 padding: 12px 0;
 box-sizing: border-box;
 transition: border-color .3s var(--n-bezier);
 `,[x("&:not(:last-child)",`
 border-bottom: 1px solid var(--n-merged-border-color);
 `)]),r("list-item",`
 position: relative;
 padding: 12px 0; 
 box-sizing: border-box;
 display: flex;
 flex-wrap: nowrap;
 align-items: center;
 transition:
 background-color .3s var(--n-bezier),
 border-color .3s var(--n-bezier);
 `,[y("prefix",`
 margin-right: 20px;
 flex: 0;
 `),y("suffix",`
 margin-left: 20px;
 flex: 0;
 `),y("main",`
 flex: 1;
 `),y("divider",`
 height: 1px;
 position: absolute;
 bottom: 0;
 left: 0;
 right: 0;
 background-color: transparent;
 transition: background-color .3s var(--n-bezier);
 pointer-events: none;
 `)])]),ne(r("list",`
 --n-merged-color-hover: var(--n-color-hover-modal);
 --n-merged-color: var(--n-color-modal);
 --n-merged-border-color: var(--n-border-color-modal);
 `)),le(r("list",`
 --n-merged-color-hover: var(--n-color-hover-popover);
 --n-merged-color: var(--n-color-popover);
 --n-merged-border-color: var(--n-border-color-popover);
 `))]),Ue=Object.assign(Object.assign({},F.props),{size:{type:String,default:"medium"},bordered:Boolean,clickable:Boolean,hoverable:Boolean,showDivider:{type:Boolean,default:!0}}),se=ge("n-list"),Ge=W({name:"List",props:Ue,slots:Object,setup(e){const{mergedClsPrefixRef:t,inlineThemeDisabled:o,mergedRtlRef:i}=Q(e),p=ie("List",i,t),v=F("List","-list",We,Fe,e,t);be(se,{showDividerRef:me(e,"showDivider"),mergedClsPrefixRef:t});const _=K(()=>{const{common:{cubicBezierEaseInOut:a},self:{fontSize:h,textColor:u,color:c,colorModal:C,colorPopover:S,borderColor:b,borderColorModal:l,borderColorPopover:$,borderRadius:z,colorHover:d,colorHoverModal:k,colorHoverPopover:P}}=v.value;return{"--n-font-size":h,"--n-bezier":a,"--n-text-color":u,"--n-color":c,"--n-border-radius":z,"--n-border-color":b,"--n-border-color-modal":l,"--n-border-color-popover":$,"--n-color-modal":C,"--n-color-popover":S,"--n-color-hover":d,"--n-color-hover-modal":k,"--n-color-hover-popover":P}}),m=o?X("list",void 0,_,e):void 0;return{mergedClsPrefix:t,rtlEnabled:p,cssVars:o?void 0:_,themeClass:m==null?void 0:m.themeClass,onRender:m==null?void 0:m.onRender}},render(){var e;const{$slots:t,mergedClsPrefix:o,onRender:i}=this;return i==null||i(),n("ul",{class:[`${o}-list`,this.rtlEnabled&&`${o}-list--rtl`,this.bordered&&`${o}-list--bordered`,this.showDivider&&`${o}-list--show-divider`,this.hoverable&&`${o}-list--hoverable`,this.clickable&&`${o}-list--clickable`,this.themeClass],style:this.cssVars},t.header?n("div",{class:`${o}-list__header`},t.header()):null,(e=t.default)===null||e===void 0?void 0:e.call(t),t.footer?n("div",{class:`${o}-list__footer`},t.footer()):null)}}),Ye=W({name:"ListItem",slots:Object,setup(){const e=he(se,null);return e||ve("list-item","`n-list-item` must be placed in `n-list`."),{showDivider:e.showDividerRef,mergedClsPrefix:e.mergedClsPrefixRef}},render(){const{$slots:e,mergedClsPrefix:t}=this;return n("li",{class:`${t}-list-item`},e.prefix?n("div",{class:`${t}-list-item__prefix`},e.prefix()):null,e.default?n("div",{class:`${t}-list-item__main`},e):null,e.suffix?n("div",{class:`${t}-list-item__suffix`},e.suffix()):null,this.showDivider&&n("div",{class:`${t}-list-item__divider`}))}}),Je=r("thing",`
 display: flex;
 transition: color .3s var(--n-bezier);
 font-size: var(--n-font-size);
 color: var(--n-text-color);
`,[r("thing-avatar",`
 margin-right: 12px;
 margin-top: 2px;
 `),r("thing-avatar-header-wrapper",`
 display: flex;
 flex-wrap: nowrap;
 `,[r("thing-header-wrapper",`
 flex: 1;
 `)]),r("thing-main",`
 flex-grow: 1;
 `,[r("thing-header",`
 display: flex;
 margin-bottom: 4px;
 justify-content: space-between;
 align-items: center;
 `,[y("title",`
 font-size: 16px;
 font-weight: var(--n-title-font-weight);
 transition: color .3s var(--n-bezier);
 color: var(--n-title-text-color);
 `)]),y("description",[x("&:not(:last-child)",`
 margin-bottom: 4px;
 `)]),y("content",[x("&:not(:first-child)",`
 margin-top: 12px;
 `)]),y("footer",[x("&:not(:first-child)",`
 margin-top: 12px;
 `)]),y("action",[x("&:not(:first-child)",`
 margin-top: 12px;
 `)])])]),qe=Object.assign(Object.assign({},F.props),{title:String,titleExtra:String,description:String,descriptionClass:String,descriptionStyle:[String,Object],content:String,contentClass:String,contentStyle:[String,Object],contentIndented:Boolean}),Ze=W({name:"Thing",props:qe,slots:Object,setup(e,{slots:t}){const{mergedClsPrefixRef:o,inlineThemeDisabled:i,mergedRtlRef:p}=Q(e),v=F("Thing","-thing",Je,Ke,e,o),_=ie("Thing",p,o),m=K(()=>{const{self:{titleTextColor:h,textColor:u,titleFontWeight:c,fontSize:C},common:{cubicBezierEaseInOut:S}}=v.value;return{"--n-bezier":S,"--n-font-size":C,"--n-text-color":u,"--n-title-font-weight":c,"--n-title-text-color":h}}),a=i?X("thing",void 0,m,e):void 0;return()=>{var h;const{value:u}=o,c=_?_.value:!1;return(h=a==null?void 0:a.onRender)===null||h===void 0||h.call(a),n("div",{class:[`${u}-thing`,a==null?void 0:a.themeClass,c&&`${u}-thing--rtl`],style:i?void 0:m.value},t.avatar&&e.contentIndented?n("div",{class:`${u}-thing-avatar`},t.avatar()):null,n("div",{class:`${u}-thing-main`},!e.contentIndented&&(t.header||e.title||t["header-extra"]||e.titleExtra||t.avatar)?n("div",{class:`${u}-thing-avatar-header-wrapper`},t.avatar?n("div",{class:`${u}-thing-avatar`},t.avatar()):null,t.header||e.title||t["header-extra"]||e.titleExtra?n("div",{class:`${u}-thing-header-wrapper`},n("div",{class:`${u}-thing-header`},t.header||e.title?n("div",{class:`${u}-thing-header__title`},t.header?t.header():e.title):null,t["header-extra"]||e.titleExtra?n("div",{class:`${u}-thing-header__extra`},t["header-extra"]?t["header-extra"]():e.titleExtra):null),t.description||e.description?n("div",{class:[`${u}-thing-main__description`,e.descriptionClass],style:e.descriptionStyle},t.description?t.description():e.description):null):null):n(q,null,t.header||e.title||t["header-extra"]||e.titleExtra?n("div",{class:`${u}-thing-header`},t.header||e.title?n("div",{class:`${u}-thing-header__title`},t.header?t.header():e.title):null,t["header-extra"]||e.titleExtra?n("div",{class:`${u}-thing-header__extra`},t["header-extra"]?t["header-extra"]():e.titleExtra):null):null,t.description||e.description?n("div",{class:[`${u}-thing-main__description`,e.descriptionClass],style:e.descriptionStyle},t.description?t.description():e.description):null),t.default||e.content?n("div",{class:[`${u}-thing-main__content`,e.contentClass],style:e.contentStyle},t.default?t.default():e.content):null,t.footer?n("div",{class:`${u}-thing-main__footer`},t.footer()):null,t.action?n("div",{class:`${u}-thing-main__action`},t.action()):null))}}}),pt={__name:"SettingsPage",setup(e){const t=we(),o=N({telegram_bot_token:"",telegram_chat_id:"",exchange_rate_api_key:""}),i=N([]),p=N([]),v=N([]),_=N(!1);async function m(){try{const b=await H.config();o.value.telegram_bot_token=b.data.telegram_bot_token||"",o.value.telegram_chat_id=b.data.telegram_chat_id||"",o.value.exchange_rate_api_key=b.data.exchange_rate_api_key||"";const l=b.data.supported_currencies||"CNY,USD,EUR,GBP,JPY,HKD,SGD,THB,KRW";i.value=l.split(",").map($=>$.trim()).filter(Boolean)}catch{}}async function a(b,l){try{await H.setConfig(b,l),t.success("保存成功")}catch{t.error("保存失败")}}async function h(){try{const b=await H.testTelegram();t.success(b.message||"推送成功")}catch{t.error("推送失败")}}async function u(){_.value=!0;try{await H.refreshRates(),t.success("刷新成功"),c()}catch{t.error("刷新失败")}finally{_.value=!1}}async function c(){try{const b=await H.rates();p.value=b.data||[]}catch{}}async function C(){try{const b=await H.alerts();v.value=b.data||[]}catch{}}async function S(b){try{await H.markAlertRead(b),C()}catch{}}return fe(()=>{m(),c(),C()}),(b,l)=>{const $=Be,z=Re,d=xe,k=Pe,P=_e,w=ke,D=je,j=He,M=Le,I=Se,O=Ze,L=Ye,A=Ge,U=ze,de=$e;return T(),V(de,{cols:2,"x-gap":16,"y-gap":16},{default:s(()=>[g(w,null,{default:s(()=>[g(P,{title:"Telegram 通知",hoverable:""},{default:s(()=>[g(k,{"label-placement":"left","label-width":"80"},{default:s(()=>[g(z,{label:"Bot Token"},{default:s(()=>[g($,{value:o.value.telegram_bot_token,"onUpdate:value":l[0]||(l[0]=f=>o.value.telegram_bot_token=f),type:"password","show-password-on":"click",placeholder:"输入 Bot Token"},null,8,["value"])]),_:1}),g(z,{label:"Chat ID"},{default:s(()=>[g($,{value:o.value.telegram_chat_id,"onUpdate:value":l[1]||(l[1]=f=>o.value.telegram_chat_id=f),placeholder:"输入 Chat ID"},null,8,["value"])]),_:1}),g(z,null,{default:s(()=>[g(d,{type:"primary",onClick:l[2]||(l[2]=f=>{a("telegram_bot_token",o.value.telegram_bot_token),a("telegram_chat_id",o.value.telegram_chat_id)})},{default:s(()=>[...l[6]||(l[6]=[E(" 保存配置 ",-1)])]),_:1}),g(d,{onClick:h,style:{"margin-left":"8px"}},{default:s(()=>[...l[7]||(l[7]=[E("推送测试",-1)])]),_:1})]),_:1})]),_:1})]),_:1})]),_:1}),g(w,null,{default:s(()=>[g(P,{title:"汇率设置",hoverable:""},{default:s(()=>[g(k,{"label-placement":"left","label-width":"80"},{default:s(()=>[g(z,{label:"API Key"},{default:s(()=>[g($,{value:o.value.exchange_rate_api_key,"onUpdate:value":l[3]||(l[3]=f=>o.value.exchange_rate_api_key=f),type:"password","show-password-on":"click",placeholder:"ExchangeRate-API Key"},null,8,["value"])]),_:1}),g(z,{label:"支持的币种"},{default:s(()=>[g(D,{value:i.value,"onUpdate:value":l[4]||(l[4]=f=>i.value=f)},null,8,["value"])]),_:1}),g(z,null,{default:s(()=>[g(d,{type:"primary",onClick:l[5]||(l[5]=f=>{a("exchange_rate_api_key",o.value.exchange_rate_api_key),a("supported_currencies",i.value.join(","))})},{default:s(()=>[...l[8]||(l[8]=[E(" 保存配置 ",-1)])]),_:1}),g(d,{onClick:u,loading:_.value,style:{"margin-left":"8px"}},{default:s(()=>[...l[9]||(l[9]=[E("手动刷新",-1)])]),_:1},8,["loading"])]),_:1})]),_:1})]),_:1})]),_:1}),g(w,null,{default:s(()=>[g(P,{title:"系统信息",hoverable:""},{default:s(()=>[g(M,{bordered:"",column:1,size:"small"},{default:s(()=>[g(j,{label:"本位币"},{default:s(()=>[...l[10]||(l[10]=[E("CNY",-1)])]),_:1}),g(j,{label:"月度起始日"},{default:s(()=>[...l[11]||(l[11]=[E("每月 1 日",-1)])]),_:1}),p.value.length>0?(T(),V(j,{key:0,label:"当前汇率"},{default:s(()=>[(T(!0),Y(q,null,ee(p.value.slice(0,5),f=>(T(),Y("div",{key:f.targetCurrency,style:{display:"flex","justify-content":"space-between",padding:"4px 0"}},[te("span",null,"1 "+J(f.targetCurrency),1),te("span",null,"¥ "+J(Number(f.rate).toFixed(4)),1)]))),128))]),_:1})):oe("",!0)]),_:1})]),_:1})]),_:1}),g(w,null,{default:s(()=>[g(P,{title:"预警日志",hoverable:""},{"header-extra":s(()=>[g(d,{size:"small",onClick:C},{default:s(()=>[...l[12]||(l[12]=[E("刷新",-1)])]),_:1})]),default:s(()=>[v.value.length>0?(T(),V(A,{key:0},{default:s(()=>[(T(!0),Y(q,null,ee(v.value,f=>(T(),V(L,{key:f.id},{prefix:s(()=>[g(I,{type:f.isSent?"success":"error",size:"small"},{default:s(()=>[E(J(f.isSent?"已推送":"未推送"),1)]),_:2},1032,["type"])]),suffix:s(()=>[f.isRead?oe("",!0):(T(),V(d,{key:0,size:"tiny",text:"",onClick:Qe=>S(f.id)},{default:s(()=>[...l[13]||(l[13]=[E("标记已读",-1)])]),_:1},8,["onClick"]))]),default:s(()=>[g(O,{title:f.title,description:f.content},null,8,["title","description"])]),_:2},1024))),128))]),_:1})):(T(),V(U,{key:1,description:"暂无预警"}))]),_:1})]),_:1})]),_:1})}}};export{pt as default};
