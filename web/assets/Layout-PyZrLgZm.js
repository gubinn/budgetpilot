import{d as P,h as s,r as B,w as Be,c as C,n as Se,t as Z,a as g,b as io,e as d,f as ao,g as _,i as m,u as so,T as co,N as Oe,j as be,k as Ae,l as uo,m as vo,o as mo,p as te,q as Y,s as Me,v as ho,x as re,y as fo,z as po,A as go,B as Ee,C as bo,D as Re,E as de,F as ne,S as je,G as Fe,H as ee,I as Le,J as G,K as D,L as oe,M as xo,O as Q,P as xe,Q as he,R as Co,U as ue,V as yo,W as $e,X as wo,Y as _o,Z as F,_ as K,$ as k,a0 as zo,a1 as E,a2 as Io,a3 as ko,a4 as So,a5 as O,a6 as Ao,a7 as Ro,a8 as q,a9 as $o}from"./index-CWyM8Sp_.js";import{_ as Ho}from"./_plugin-vue_export-helper-DlAUqK2U.js";import{d as Po,t as No,C as To,N as Bo,a as Oo}from"./Dropdown-xvi64wus.js";import{f as ve,u as fe,a as Mo}from"./get-78qTiK13.js";import{g as Eo,V as jo,c as me}from"./Popover-Bqj9tlk9.js";import{_ as Fo}from"./Space-Ciajk8Hb.js";import{N as ae}from"./Icon-CDyrUQDu.js";import"./use-keyboard--BYedXkK.js";const Lo=P({name:"ChevronDownFilled",render(){return s("svg",{viewBox:"0 0 16 16",fill:"none",xmlns:"http://www.w3.org/2000/svg"},s("path",{d:"M3.20041 5.73966C3.48226 5.43613 3.95681 5.41856 4.26034 5.70041L8 9.22652L11.7397 5.70041C12.0432 5.41856 12.5177 5.43613 12.7996 5.73966C13.0815 6.0432 13.0639 6.51775 12.7603 6.7996L8.51034 10.7996C8.22258 11.0668 7.77743 11.0668 7.48967 10.7996L3.23966 6.7996C2.93613 6.51775 2.91856 6.0432 3.20041 5.73966Z",fill:"currentColor"}))}}),He=P({name:"SlotMachineNumber",props:{clsPrefix:{type:String,required:!0},value:{type:[Number,String],required:!0},oldOriginalNumber:{type:Number,default:void 0},newOriginalNumber:{type:Number,default:void 0}},setup(e){const t=B(null),o=B(e.value),r=B(e.value),i=B("up"),l=B(!1),u=C(()=>l.value?`${e.clsPrefix}-base-slot-machine-current-number--${i.value}-scroll`:null),v=C(()=>l.value?`${e.clsPrefix}-base-slot-machine-old-number--${i.value}-scroll`:null);Be(Z(e,"value"),(x,b)=>{o.value=b,r.value=x,Se(c)});function c(){const x=e.newOriginalNumber,b=e.oldOriginalNumber;b===void 0||x===void 0||(x>b?f("up"):b>x&&f("down"))}function f(x){i.value=x,l.value=!1,Se(()=>{var b;(b=t.value)===null||b===void 0||b.offsetWidth,l.value=!0})}return()=>{const{clsPrefix:x}=e;return s("span",{ref:t,class:`${x}-base-slot-machine-number`},o.value!==null?s("span",{class:[`${x}-base-slot-machine-old-number ${x}-base-slot-machine-old-number--top`,v.value]},o.value):null,s("span",{class:[`${x}-base-slot-machine-current-number`,u.value]},s("span",{ref:"numberWrapper",class:[`${x}-base-slot-machine-current-number__inner`,typeof e.value!="number"&&`${x}-base-slot-machine-current-number__inner--not-number`]},r.value)),o.value!==null?s("span",{class:[`${x}-base-slot-machine-old-number ${x}-base-slot-machine-old-number--bottom`,v.value]},o.value):null)}}}),{cubicBezierEaseOut:J}=io;function Vo({duration:e=".2s"}={}){return[g("&.fade-up-width-expand-transition-leave-active",{transition:`
 opacity ${e} ${J},
 max-width ${e} ${J},
 transform ${e} ${J}
 `}),g("&.fade-up-width-expand-transition-enter-active",{transition:`
 opacity ${e} ${J},
 max-width ${e} ${J},
 transform ${e} ${J}
 `}),g("&.fade-up-width-expand-transition-enter-to",{opacity:1,transform:"translateX(0) translateY(0)"}),g("&.fade-up-width-expand-transition-enter-from",{maxWidth:"0 !important",opacity:0,transform:"translateY(60%)"}),g("&.fade-up-width-expand-transition-leave-from",{opacity:1,transform:"translateY(0)"}),g("&.fade-up-width-expand-transition-leave-to",{maxWidth:"0 !important",opacity:0,transform:"translateY(60%)"})]}const Ko=g([g("@keyframes n-base-slot-machine-fade-up-in",`
 from {
 transform: translateY(60%);
 opacity: 0;
 }
 to {
 transform: translateY(0);
 opacity: 1;
 }
 `),g("@keyframes n-base-slot-machine-fade-down-in",`
 from {
 transform: translateY(-60%);
 opacity: 0;
 }
 to {
 transform: translateY(0);
 opacity: 1;
 }
 `),g("@keyframes n-base-slot-machine-fade-up-out",`
 from {
 transform: translateY(0%);
 opacity: 1;
 }
 to {
 transform: translateY(-60%);
 opacity: 0;
 }
 `),g("@keyframes n-base-slot-machine-fade-down-out",`
 from {
 transform: translateY(0%);
 opacity: 1;
 }
 to {
 transform: translateY(60%);
 opacity: 0;
 }
 `),d("base-slot-machine",`
 overflow: hidden;
 white-space: nowrap;
 display: inline-block;
 height: 18px;
 line-height: 18px;
 `,[d("base-slot-machine-number",`
 display: inline-block;
 position: relative;
 height: 18px;
 width: .6em;
 max-width: .6em;
 `,[Vo({duration:".2s"}),ao({duration:".2s",delay:"0s"}),d("base-slot-machine-old-number",`
 display: inline-block;
 opacity: 0;
 position: absolute;
 left: 0;
 right: 0;
 `,[_("top",{transform:"translateY(-100%)"}),_("bottom",{transform:"translateY(100%)"}),_("down-scroll",{animation:"n-base-slot-machine-fade-down-out .2s cubic-bezier(0, 0, .2, 1)",animationIterationCount:1}),_("up-scroll",{animation:"n-base-slot-machine-fade-up-out .2s cubic-bezier(0, 0, .2, 1)",animationIterationCount:1})]),d("base-slot-machine-current-number",`
 display: inline-block;
 position: absolute;
 left: 0;
 top: 0;
 bottom: 0;
 right: 0;
 opacity: 1;
 transform: translateY(0);
 width: .6em;
 `,[_("down-scroll",{animation:"n-base-slot-machine-fade-down-in .2s cubic-bezier(0, 0, .2, 1)",animationIterationCount:1}),_("up-scroll",{animation:"n-base-slot-machine-fade-up-in .2s cubic-bezier(0, 0, .2, 1)",animationIterationCount:1}),m("inner",`
 display: inline-block;
 position: absolute;
 right: 0;
 top: 0;
 width: .6em;
 `,[_("not-number",`
 right: unset;
 left: 0;
 `)])])])])]),Do=P({name:"BaseSlotMachine",props:{clsPrefix:{type:String,required:!0},value:{type:[Number,String],default:0},max:{type:Number,default:void 0},appeared:{type:Boolean,required:!0}},setup(e){so("-base-slot-machine",Ko,Z(e,"clsPrefix"));const t=B(),o=B(),r=C(()=>{if(typeof e.value=="string")return[];if(e.value<1)return[0];const i=[];let l=e.value;for(e.max!==void 0&&(l=Math.min(e.max,l));l>=1;)i.push(l%10),l/=10,l=Math.floor(l);return i.reverse(),i});return Be(Z(e,"value"),(i,l)=>{typeof i=="string"?(o.value=void 0,t.value=void 0):typeof l=="string"?(o.value=i,t.value=void 0):(o.value=i,t.value=l)}),()=>{const{value:i,clsPrefix:l}=e;return typeof i=="number"?s("span",{class:`${l}-base-slot-machine`},s(co,{name:"fade-up-width-expand-transition",tag:"span"},{default:()=>r.value.map((u,v)=>s(He,{clsPrefix:l,key:r.value.length-v-1,oldOriginalNumber:t.value,newOriginalNumber:o.value,value:u}))}),s(Oe,{key:"+",width:!0},{default:()=>e.max!==void 0&&e.max<i?s(He,{clsPrefix:l,value:"+"}):null})):s("span",{class:`${l}-base-slot-machine`},i)}}});function Yo(e){const{errorColor:t,infoColor:o,successColor:r,warningColor:i,fontFamily:l}=e;return{color:t,colorInfo:o,colorSuccess:r,colorError:t,colorWarning:i,fontSize:"12px",fontFamily:l}}const Uo={common:be,self:Yo},Wo=g([g("@keyframes badge-wave-spread",{from:{boxShadow:"0 0 0.5px 0px var(--n-ripple-color)",opacity:.6},to:{boxShadow:"0 0 0.5px 4.5px var(--n-ripple-color)",opacity:0}}),d("badge",`
 display: inline-flex;
 position: relative;
 vertical-align: middle;
 font-family: var(--n-font-family);
 `,[_("as-is",[d("badge-sup",{position:"static",transform:"translateX(0)"},[Ae({transformOrigin:"left bottom",originalTransform:"translateX(0)"})])]),_("dot",[d("badge-sup",`
 height: 8px;
 width: 8px;
 padding: 0;
 min-width: 8px;
 left: 100%;
 bottom: calc(100% - 4px);
 `,[g("::before","border-radius: 4px;")])]),d("badge-sup",`
 background: var(--n-color);
 transition:
 background-color .3s var(--n-bezier),
 color .3s var(--n-bezier);
 color: #FFF;
 position: absolute;
 height: 18px;
 line-height: 18px;
 border-radius: 9px;
 padding: 0 6px;
 text-align: center;
 font-size: var(--n-font-size);
 transform: translateX(-50%);
 left: 100%;
 bottom: calc(100% - 9px);
 font-variant-numeric: tabular-nums;
 z-index: 2;
 display: flex;
 align-items: center;
 `,[Ae({transformOrigin:"left bottom",originalTransform:"translateX(-50%)"}),d("base-wave",{zIndex:1,animationDuration:"2s",animationIterationCount:"infinite",animationDelay:"1s",animationTimingFunction:"var(--n-ripple-bezier)",animationName:"badge-wave-spread"}),g("&::before",`
 opacity: 0;
 transform: scale(1);
 border-radius: 9px;
 content: "";
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 `)])])]),qo=Object.assign(Object.assign({},Y.props),{value:[String,Number],max:Number,dot:Boolean,type:{type:String,default:"default"},show:{type:Boolean,default:!0},showZero:Boolean,processing:Boolean,color:String,offset:Array}),Go=P({name:"Badge",props:qo,setup(e,{slots:t}){const{mergedClsPrefixRef:o,inlineThemeDisabled:r,mergedRtlRef:i}=te(e),l=Y("Badge","-badge",Wo,Uo,e,o),u=B(!1),v=()=>{u.value=!0},c=()=>{u.value=!1},f=C(()=>e.show&&(e.dot||e.value!==void 0&&!(!e.showZero&&Number(e.value)<=0)||!fo(t.value)));Me(()=>{f.value&&(u.value=!0)});const x=ho("Badge",i,o),b=C(()=>{const{type:I,color:y}=e,{common:{cubicBezierEaseInOut:z,cubicBezierEaseOut:$},self:{[po("color",I)]:L,fontFamily:V,fontSize:M}}=l.value;return{"--n-font-size":M,"--n-font-family":V,"--n-color":y||L,"--n-ripple-color":y||L,"--n-bezier":z,"--n-ripple-bezier":$}}),h=r?re("badge",C(()=>{let I="";const{type:y,color:z}=e;return y&&(I+=y[0]),z&&(I+=go(z)),I}),b,e):void 0,N=C(()=>{const{offset:I}=e;if(!I)return;const[y,z]=I,$=typeof y=="number"?`${y}px`:y,L=typeof z=="number"?`${z}px`:z;return{transform:`translate(calc(${x!=null&&x.value?"50%":"-50%"} + ${$}), ${L})`}});return{rtlEnabled:x,mergedClsPrefix:o,appeared:u,showBadge:f,handleAfterEnter:v,handleAfterLeave:c,cssVars:r?void 0:b,themeClass:h==null?void 0:h.themeClass,onRender:h==null?void 0:h.onRender,offsetStyle:N}},render(){var e;const{mergedClsPrefix:t,onRender:o,themeClass:r,$slots:i}=this;o==null||o();const l=(e=i.default)===null||e===void 0?void 0:e.call(i);return s("div",{class:[`${t}-badge`,this.rtlEnabled&&`${t}-badge--rtl`,r,{[`${t}-badge--dot`]:this.dot,[`${t}-badge--as-is`]:!l}],style:this.cssVars},l,s(uo,{name:"fade-in-scale-up-transition",onAfterEnter:this.handleAfterEnter,onAfterLeave:this.handleAfterLeave},{default:()=>this.showBadge?s("sup",{class:`${t}-badge-sup`,title:Eo(this.value),style:this.offsetStyle},vo(i.value,()=>[this.dot?null:s(Do,{clsPrefix:t,appeared:this.appeared,max:this.max,value:this.value})]),this.processing?s(mo,{clsPrefix:t}):null):null}))}});function Xo(e){const{baseColor:t,textColor2:o,bodyColor:r,cardColor:i,dividerColor:l,actionColor:u,scrollbarColor:v,scrollbarColorHover:c,invertedColor:f}=e;return{textColor:o,textColorInverted:"#FFF",color:r,colorEmbedded:u,headerColor:i,headerColorInverted:f,footerColor:u,footerColorInverted:f,headerBorderColor:l,headerBorderColorInverted:f,footerBorderColor:l,footerBorderColorInverted:f,siderBorderColor:l,siderBorderColorInverted:f,siderColor:i,siderColorInverted:f,siderToggleButtonBorder:`1px solid ${l}`,siderToggleButtonColor:t,siderToggleButtonIconColor:o,siderToggleButtonIconColorInverted:o,siderToggleBarColor:Re(r,v),siderToggleBarColorHover:Re(r,c),__invertScrollbar:"true"}}const Ce=Ee({name:"Layout",common:be,peers:{Scrollbar:bo},self:Xo});function Zo(e,t,o,r){return{itemColorHoverInverted:"#0000",itemColorActiveInverted:t,itemColorActiveHoverInverted:t,itemColorActiveCollapsedInverted:t,itemTextColorInverted:e,itemTextColorHoverInverted:o,itemTextColorChildActiveInverted:o,itemTextColorChildActiveHoverInverted:o,itemTextColorActiveInverted:o,itemTextColorActiveHoverInverted:o,itemTextColorHorizontalInverted:e,itemTextColorHoverHorizontalInverted:o,itemTextColorChildActiveHorizontalInverted:o,itemTextColorChildActiveHoverHorizontalInverted:o,itemTextColorActiveHorizontalInverted:o,itemTextColorActiveHoverHorizontalInverted:o,itemIconColorInverted:e,itemIconColorHoverInverted:o,itemIconColorActiveInverted:o,itemIconColorActiveHoverInverted:o,itemIconColorChildActiveInverted:o,itemIconColorChildActiveHoverInverted:o,itemIconColorCollapsedInverted:e,itemIconColorHorizontalInverted:e,itemIconColorHoverHorizontalInverted:o,itemIconColorActiveHorizontalInverted:o,itemIconColorActiveHoverHorizontalInverted:o,itemIconColorChildActiveHorizontalInverted:o,itemIconColorChildActiveHoverHorizontalInverted:o,arrowColorInverted:e,arrowColorHoverInverted:o,arrowColorActiveInverted:o,arrowColorActiveHoverInverted:o,arrowColorChildActiveInverted:o,arrowColorChildActiveHoverInverted:o,groupTextColorInverted:r}}function Jo(e){const{borderRadius:t,textColor3:o,primaryColor:r,textColor2:i,textColor1:l,fontSize:u,dividerColor:v,hoverColor:c,primaryColorHover:f}=e;return Object.assign({borderRadius:t,color:"#0000",groupTextColor:o,itemColorHover:c,itemColorActive:de(r,{alpha:.1}),itemColorActiveHover:de(r,{alpha:.1}),itemColorActiveCollapsed:de(r,{alpha:.1}),itemTextColor:i,itemTextColorHover:i,itemTextColorActive:r,itemTextColorActiveHover:r,itemTextColorChildActive:r,itemTextColorChildActiveHover:r,itemTextColorHorizontal:i,itemTextColorHoverHorizontal:f,itemTextColorActiveHorizontal:r,itemTextColorActiveHoverHorizontal:r,itemTextColorChildActiveHorizontal:r,itemTextColorChildActiveHoverHorizontal:r,itemIconColor:l,itemIconColorHover:l,itemIconColorActive:r,itemIconColorActiveHover:r,itemIconColorChildActive:r,itemIconColorChildActiveHover:r,itemIconColorCollapsed:l,itemIconColorHorizontal:l,itemIconColorHoverHorizontal:f,itemIconColorActiveHorizontal:r,itemIconColorActiveHoverHorizontal:r,itemIconColorChildActiveHorizontal:r,itemIconColorChildActiveHoverHorizontal:r,itemHeight:"42px",arrowColor:i,arrowColorHover:i,arrowColorActive:r,arrowColorActiveHover:r,arrowColorChildActive:r,arrowColorChildActiveHover:r,colorInverted:"#0000",borderColorHorizontal:"#0000",fontSize:u,dividerColor:v},Zo("#BBB",r,"#FFF","#AAA"))}const Qo=Ee({name:"Menu",common:be,peers:{Tooltip:No,Dropdown:Po},self:Jo}),Ve=ne("n-layout-sider"),ye={type:String,default:"static"},et=d("layout",`
 color: var(--n-text-color);
 background-color: var(--n-color);
 box-sizing: border-box;
 position: relative;
 z-index: auto;
 flex: auto;
 overflow: hidden;
 transition:
 box-shadow .3s var(--n-bezier),
 background-color .3s var(--n-bezier),
 color .3s var(--n-bezier);
`,[d("layout-scroll-container",`
 overflow-x: hidden;
 box-sizing: border-box;
 height: 100%;
 `),_("absolute-positioned",`
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 `)]),ot={embedded:Boolean,position:ye,nativeScrollbar:{type:Boolean,default:!0},scrollbarProps:Object,onScroll:Function,contentClass:String,contentStyle:{type:[String,Object],default:""},hasSider:Boolean,siderPlacement:{type:String,default:"left"}},Ke=ne("n-layout");function De(e){return P({name:e?"LayoutContent":"Layout",props:Object.assign(Object.assign({},Y.props),ot),setup(t){const o=B(null),r=B(null),{mergedClsPrefixRef:i,inlineThemeDisabled:l}=te(t),u=Y("Layout","-layout",et,Ce,t,i);function v(y,z){if(t.nativeScrollbar){const{value:$}=o;$&&(z===void 0?$.scrollTo(y):$.scrollTo(y,z))}else{const{value:$}=r;$&&$.scrollTo(y,z)}}ee(Ke,t);let c=0,f=0;const x=y=>{var z;const $=y.target;c=$.scrollLeft,f=$.scrollTop,(z=t.onScroll)===null||z===void 0||z.call(t,y)};Fe(()=>{if(t.nativeScrollbar){const y=o.value;y&&(y.scrollTop=f,y.scrollLeft=c)}});const b={display:"flex",flexWrap:"nowrap",width:"100%",flexDirection:"row"},h={scrollTo:v},N=C(()=>{const{common:{cubicBezierEaseInOut:y},self:z}=u.value;return{"--n-bezier":y,"--n-color":t.embedded?z.colorEmbedded:z.color,"--n-text-color":z.textColor}}),I=l?re("layout",C(()=>t.embedded?"e":""),N,t):void 0;return Object.assign({mergedClsPrefix:i,scrollableElRef:o,scrollbarInstRef:r,hasSiderStyle:b,mergedTheme:u,handleNativeElScroll:x,cssVars:l?void 0:N,themeClass:I==null?void 0:I.themeClass,onRender:I==null?void 0:I.onRender},h)},render(){var t;const{mergedClsPrefix:o,hasSider:r}=this;(t=this.onRender)===null||t===void 0||t.call(this);const i=r?this.hasSiderStyle:void 0,l=[this.themeClass,e&&`${o}-layout-content`,`${o}-layout`,`${o}-layout--${this.position}-positioned`];return s("div",{class:l,style:this.cssVars},this.nativeScrollbar?s("div",{ref:"scrollableElRef",class:[`${o}-layout-scroll-container`,this.contentClass],style:[this.contentStyle,i],onScroll:this.handleNativeElScroll},this.$slots):s(je,Object.assign({},this.scrollbarProps,{onScroll:this.onScroll,ref:"scrollbarInstRef",theme:this.mergedTheme.peers.Scrollbar,themeOverrides:this.mergedTheme.peerOverrides.Scrollbar,contentClass:this.contentClass,contentStyle:[this.contentStyle,i]}),this.$slots))}})}const tt=De(!1),rt=De(!0),nt=d("layout-header",`
 transition:
 color .3s var(--n-bezier),
 background-color .3s var(--n-bezier),
 box-shadow .3s var(--n-bezier),
 border-color .3s var(--n-bezier);
 box-sizing: border-box;
 width: 100%;
 background-color: var(--n-color);
 color: var(--n-text-color);
`,[_("absolute-positioned",`
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 `),_("bordered",`
 border-bottom: solid 1px var(--n-border-color);
 `)]),lt={position:ye,inverted:Boolean,bordered:{type:Boolean,default:!1}},it=P({name:"LayoutHeader",props:Object.assign(Object.assign({},Y.props),lt),setup(e){const{mergedClsPrefixRef:t,inlineThemeDisabled:o}=te(e),r=Y("Layout","-layout-header",nt,Ce,e,t),i=C(()=>{const{common:{cubicBezierEaseInOut:u},self:v}=r.value,c={"--n-bezier":u};return e.inverted?(c["--n-color"]=v.headerColorInverted,c["--n-text-color"]=v.textColorInverted,c["--n-border-color"]=v.headerBorderColorInverted):(c["--n-color"]=v.headerColor,c["--n-text-color"]=v.textColor,c["--n-border-color"]=v.headerBorderColor),c}),l=o?re("layout-header",C(()=>e.inverted?"a":"b"),i,e):void 0;return{mergedClsPrefix:t,cssVars:o?void 0:i,themeClass:l==null?void 0:l.themeClass,onRender:l==null?void 0:l.onRender}},render(){var e;const{mergedClsPrefix:t}=this;return(e=this.onRender)===null||e===void 0||e.call(this),s("div",{class:[`${t}-layout-header`,this.themeClass,this.position&&`${t}-layout-header--${this.position}-positioned`,this.bordered&&`${t}-layout-header--bordered`],style:this.cssVars},this.$slots)}}),at=d("layout-sider",`
 flex-shrink: 0;
 box-sizing: border-box;
 position: relative;
 z-index: 1;
 color: var(--n-text-color);
 transition:
 color .3s var(--n-bezier),
 border-color .3s var(--n-bezier),
 min-width .3s var(--n-bezier),
 max-width .3s var(--n-bezier),
 transform .3s var(--n-bezier),
 background-color .3s var(--n-bezier);
 background-color: var(--n-color);
 display: flex;
 justify-content: flex-end;
`,[_("bordered",[m("border",`
 content: "";
 position: absolute;
 top: 0;
 bottom: 0;
 width: 1px;
 background-color: var(--n-border-color);
 transition: background-color .3s var(--n-bezier);
 `)]),m("left-placement",[_("bordered",[m("border",`
 right: 0;
 `)])]),_("right-placement",`
 justify-content: flex-start;
 `,[_("bordered",[m("border",`
 left: 0;
 `)]),_("collapsed",[d("layout-toggle-button",[d("base-icon",`
 transform: rotate(180deg);
 `)]),d("layout-toggle-bar",[g("&:hover",[m("top",{transform:"rotate(-12deg) scale(1.15) translateY(-2px)"}),m("bottom",{transform:"rotate(12deg) scale(1.15) translateY(2px)"})])])]),d("layout-toggle-button",`
 left: 0;
 transform: translateX(-50%) translateY(-50%);
 `,[d("base-icon",`
 transform: rotate(0);
 `)]),d("layout-toggle-bar",`
 left: -28px;
 transform: rotate(180deg);
 `,[g("&:hover",[m("top",{transform:"rotate(12deg) scale(1.15) translateY(-2px)"}),m("bottom",{transform:"rotate(-12deg) scale(1.15) translateY(2px)"})])])]),_("collapsed",[d("layout-toggle-bar",[g("&:hover",[m("top",{transform:"rotate(-12deg) scale(1.15) translateY(-2px)"}),m("bottom",{transform:"rotate(12deg) scale(1.15) translateY(2px)"})])]),d("layout-toggle-button",[d("base-icon",`
 transform: rotate(0);
 `)])]),d("layout-toggle-button",`
 transition:
 color .3s var(--n-bezier),
 right .3s var(--n-bezier),
 left .3s var(--n-bezier),
 border-color .3s var(--n-bezier),
 background-color .3s var(--n-bezier);
 cursor: pointer;
 width: 24px;
 height: 24px;
 position: absolute;
 top: 50%;
 right: 0;
 border-radius: 50%;
 display: flex;
 align-items: center;
 justify-content: center;
 font-size: 18px;
 color: var(--n-toggle-button-icon-color);
 border: var(--n-toggle-button-border);
 background-color: var(--n-toggle-button-color);
 box-shadow: 0 2px 4px 0px rgba(0, 0, 0, .06);
 transform: translateX(50%) translateY(-50%);
 z-index: 1;
 `,[d("base-icon",`
 transition: transform .3s var(--n-bezier);
 transform: rotate(180deg);
 `)]),d("layout-toggle-bar",`
 cursor: pointer;
 height: 72px;
 width: 32px;
 position: absolute;
 top: calc(50% - 36px);
 right: -28px;
 `,[m("top, bottom",`
 position: absolute;
 width: 4px;
 border-radius: 2px;
 height: 38px;
 left: 14px;
 transition: 
 background-color .3s var(--n-bezier),
 transform .3s var(--n-bezier);
 `),m("bottom",`
 position: absolute;
 top: 34px;
 `),g("&:hover",[m("top",{transform:"rotate(12deg) scale(1.15) translateY(-2px)"}),m("bottom",{transform:"rotate(-12deg) scale(1.15) translateY(2px)"})]),m("top, bottom",{backgroundColor:"var(--n-toggle-bar-color)"}),g("&:hover",[m("top, bottom",{backgroundColor:"var(--n-toggle-bar-color-hover)"})])]),m("border",`
 position: absolute;
 top: 0;
 right: 0;
 bottom: 0;
 width: 1px;
 transition: background-color .3s var(--n-bezier);
 `),d("layout-sider-scroll-container",`
 flex-grow: 1;
 flex-shrink: 0;
 box-sizing: border-box;
 height: 100%;
 opacity: 0;
 transition: opacity .3s var(--n-bezier);
 max-width: 100%;
 `),_("show-content",[d("layout-sider-scroll-container",{opacity:1})]),_("absolute-positioned",`
 position: absolute;
 left: 0;
 top: 0;
 bottom: 0;
 `)]),st=P({props:{clsPrefix:{type:String,required:!0},onClick:Function},render(){const{clsPrefix:e}=this;return s("div",{onClick:this.onClick,class:`${e}-layout-toggle-bar`},s("div",{class:`${e}-layout-toggle-bar__top`}),s("div",{class:`${e}-layout-toggle-bar__bottom`}))}}),ct=P({name:"LayoutToggleButton",props:{clsPrefix:{type:String,required:!0},onClick:Function},render(){const{clsPrefix:e}=this;return s("div",{class:`${e}-layout-toggle-button`,onClick:this.onClick},s(Le,{clsPrefix:e},{default:()=>s(To,null)}))}}),dt={position:ye,bordered:Boolean,collapsedWidth:{type:Number,default:48},width:{type:[Number,String],default:272},contentClass:String,contentStyle:{type:[String,Object],default:""},collapseMode:{type:String,default:"transform"},collapsed:{type:Boolean,default:void 0},defaultCollapsed:Boolean,showCollapsedContent:{type:Boolean,default:!0},showTrigger:{type:[Boolean,String],default:!1},nativeScrollbar:{type:Boolean,default:!0},inverted:Boolean,scrollbarProps:Object,triggerClass:String,triggerStyle:[String,Object],collapsedTriggerClass:String,collapsedTriggerStyle:[String,Object],"onUpdate:collapsed":[Function,Array],onUpdateCollapsed:[Function,Array],onAfterEnter:Function,onAfterLeave:Function,onExpand:[Function,Array],onCollapse:[Function,Array],onScroll:Function},ut=P({name:"LayoutSider",props:Object.assign(Object.assign({},Y.props),dt),setup(e){const t=G(Ke),o=B(null),r=B(null),i=B(e.defaultCollapsed),l=fe(Z(e,"collapsed"),i),u=C(()=>ve(l.value?e.collapsedWidth:e.width)),v=C(()=>e.collapseMode!=="transform"?{}:{minWidth:ve(e.width)}),c=C(()=>t?t.siderPlacement:"left");function f(H,A){if(e.nativeScrollbar){const{value:R}=o;R&&(A===void 0?R.scrollTo(H):R.scrollTo(H,A))}else{const{value:R}=r;R&&R.scrollTo(H,A)}}function x(){const{"onUpdate:collapsed":H,onUpdateCollapsed:A,onExpand:R,onCollapse:W}=e,{value:U}=l;A&&D(A,!U),H&&D(H,!U),i.value=!U,U?R&&D(R):W&&D(W)}let b=0,h=0;const N=H=>{var A;const R=H.target;b=R.scrollLeft,h=R.scrollTop,(A=e.onScroll)===null||A===void 0||A.call(e,H)};Fe(()=>{if(e.nativeScrollbar){const H=o.value;H&&(H.scrollTop=h,H.scrollLeft=b)}}),ee(Ve,{collapsedRef:l,collapseModeRef:Z(e,"collapseMode")});const{mergedClsPrefixRef:I,inlineThemeDisabled:y}=te(e),z=Y("Layout","-layout-sider",at,Ce,e,I);function $(H){var A,R;H.propertyName==="max-width"&&(l.value?(A=e.onAfterLeave)===null||A===void 0||A.call(e):(R=e.onAfterEnter)===null||R===void 0||R.call(e))}const L={scrollTo:f},V=C(()=>{const{common:{cubicBezierEaseInOut:H},self:A}=z.value,{siderToggleButtonColor:R,siderToggleButtonBorder:W,siderToggleBarColor:U,siderToggleBarColorHover:ce}=A,j={"--n-bezier":H,"--n-toggle-button-color":R,"--n-toggle-button-border":W,"--n-toggle-bar-color":U,"--n-toggle-bar-color-hover":ce};return e.inverted?(j["--n-color"]=A.siderColorInverted,j["--n-text-color"]=A.textColorInverted,j["--n-border-color"]=A.siderBorderColorInverted,j["--n-toggle-button-icon-color"]=A.siderToggleButtonIconColorInverted,j.__invertScrollbar=A.__invertScrollbar):(j["--n-color"]=A.siderColor,j["--n-text-color"]=A.textColor,j["--n-border-color"]=A.siderBorderColor,j["--n-toggle-button-icon-color"]=A.siderToggleButtonIconColor),j}),M=y?re("layout-sider",C(()=>e.inverted?"a":"b"),V,e):void 0;return Object.assign({scrollableElRef:o,scrollbarInstRef:r,mergedClsPrefix:I,mergedTheme:z,styleMaxWidth:u,mergedCollapsed:l,scrollContainerStyle:v,siderPlacement:c,handleNativeElScroll:N,handleTransitionend:$,handleTriggerClick:x,inlineThemeDisabled:y,cssVars:V,themeClass:M==null?void 0:M.themeClass,onRender:M==null?void 0:M.onRender},L)},render(){var e;const{mergedClsPrefix:t,mergedCollapsed:o,showTrigger:r}=this;return(e=this.onRender)===null||e===void 0||e.call(this),s("aside",{class:[`${t}-layout-sider`,this.themeClass,`${t}-layout-sider--${this.position}-positioned`,`${t}-layout-sider--${this.siderPlacement}-placement`,this.bordered&&`${t}-layout-sider--bordered`,o&&`${t}-layout-sider--collapsed`,(!o||this.showCollapsedContent)&&`${t}-layout-sider--show-content`],onTransitionend:this.handleTransitionend,style:[this.inlineThemeDisabled?void 0:this.cssVars,{maxWidth:this.styleMaxWidth,width:ve(this.width)}]},this.nativeScrollbar?s("div",{class:[`${t}-layout-sider-scroll-container`,this.contentClass],onScroll:this.handleNativeElScroll,style:[this.scrollContainerStyle,{overflow:"auto"},this.contentStyle],ref:"scrollableElRef"},this.$slots):s(je,Object.assign({},this.scrollbarProps,{onScroll:this.onScroll,ref:"scrollbarInstRef",style:this.scrollContainerStyle,contentStyle:this.contentStyle,contentClass:this.contentClass,theme:this.mergedTheme.peers.Scrollbar,themeOverrides:this.mergedTheme.peerOverrides.Scrollbar,builtinThemeOverrides:this.inverted&&this.cssVars.__invertScrollbar==="true"?{colorHover:"rgba(255, 255, 255, .4)",color:"rgba(255, 255, 255, .3)"}:void 0}),this.$slots),r?r==="bar"?s(st,{clsPrefix:t,class:o?this.collapsedTriggerClass:this.triggerClass,style:o?this.collapsedTriggerStyle:this.triggerStyle,onClick:this.handleTriggerClick}):s(ct,{clsPrefix:t,class:o?this.collapsedTriggerClass:this.triggerClass,style:o?this.collapsedTriggerStyle:this.triggerStyle,onClick:this.handleTriggerClick}):null,this.bordered?s("div",{class:`${t}-layout-sider__border`}):null)}}),le=ne("n-menu"),Ye=ne("n-submenu"),we=ne("n-menu-item-group"),Pe=[g("&::before","background-color: var(--n-item-color-hover);"),m("arrow",`
 color: var(--n-arrow-color-hover);
 `),m("icon",`
 color: var(--n-item-icon-color-hover);
 `),d("menu-item-content-header",`
 color: var(--n-item-text-color-hover);
 `,[g("a",`
 color: var(--n-item-text-color-hover);
 `),m("extra",`
 color: var(--n-item-text-color-hover);
 `)])],Ne=[m("icon",`
 color: var(--n-item-icon-color-hover-horizontal);
 `),d("menu-item-content-header",`
 color: var(--n-item-text-color-hover-horizontal);
 `,[g("a",`
 color: var(--n-item-text-color-hover-horizontal);
 `),m("extra",`
 color: var(--n-item-text-color-hover-horizontal);
 `)])],vt=g([d("menu",`
 background-color: var(--n-color);
 color: var(--n-item-text-color);
 overflow: hidden;
 transition: background-color .3s var(--n-bezier);
 box-sizing: border-box;
 font-size: var(--n-font-size);
 padding-bottom: 6px;
 `,[_("horizontal",`
 max-width: 100%;
 width: 100%;
 display: flex;
 overflow: hidden;
 padding-bottom: 0;
 `,[d("submenu","margin: 0;"),d("menu-item","margin: 0;"),d("menu-item-content",`
 padding: 0 20px;
 border-bottom: 2px solid #0000;
 `,[g("&::before","display: none;"),_("selected","border-bottom: 2px solid var(--n-border-color-horizontal)")]),d("menu-item-content",[_("selected",[m("icon","color: var(--n-item-icon-color-active-horizontal);"),d("menu-item-content-header",`
 color: var(--n-item-text-color-active-horizontal);
 `,[g("a","color: var(--n-item-text-color-active-horizontal);"),m("extra","color: var(--n-item-text-color-active-horizontal);")])]),_("child-active",`
 border-bottom: 2px solid var(--n-border-color-horizontal);
 `,[d("menu-item-content-header",`
 color: var(--n-item-text-color-child-active-horizontal);
 `,[g("a",`
 color: var(--n-item-text-color-child-active-horizontal);
 `),m("extra",`
 color: var(--n-item-text-color-child-active-horizontal);
 `)]),m("icon",`
 color: var(--n-item-icon-color-child-active-horizontal);
 `)]),oe("disabled",[oe("selected, child-active",[g("&:focus-within",Ne)]),_("selected",[X(null,[m("icon","color: var(--n-item-icon-color-active-hover-horizontal);"),d("menu-item-content-header",`
 color: var(--n-item-text-color-active-hover-horizontal);
 `,[g("a","color: var(--n-item-text-color-active-hover-horizontal);"),m("extra","color: var(--n-item-text-color-active-hover-horizontal);")])])]),_("child-active",[X(null,[m("icon","color: var(--n-item-icon-color-child-active-hover-horizontal);"),d("menu-item-content-header",`
 color: var(--n-item-text-color-child-active-hover-horizontal);
 `,[g("a","color: var(--n-item-text-color-child-active-hover-horizontal);"),m("extra","color: var(--n-item-text-color-child-active-hover-horizontal);")])])]),X("border-bottom: 2px solid var(--n-border-color-horizontal);",Ne)]),d("menu-item-content-header",[g("a","color: var(--n-item-text-color-horizontal);")])])]),oe("responsive",[d("menu-item-content-header",`
 overflow: hidden;
 text-overflow: ellipsis;
 `)]),_("collapsed",[d("menu-item-content",[_("selected",[g("&::before",`
 background-color: var(--n-item-color-active-collapsed) !important;
 `)]),d("menu-item-content-header","opacity: 0;"),m("arrow","opacity: 0;"),m("icon","color: var(--n-item-icon-color-collapsed);")])]),d("menu-item",`
 height: var(--n-item-height);
 margin-top: 6px;
 position: relative;
 `),d("menu-item-content",`
 box-sizing: border-box;
 line-height: 1.75;
 height: 100%;
 display: grid;
 grid-template-areas: "icon content arrow";
 grid-template-columns: auto 1fr auto;
 align-items: center;
 cursor: pointer;
 position: relative;
 padding-right: 18px;
 transition:
 background-color .3s var(--n-bezier),
 padding-left .3s var(--n-bezier),
 border-color .3s var(--n-bezier);
 `,[g("> *","z-index: 1;"),g("&::before",`
 z-index: auto;
 content: "";
 background-color: #0000;
 position: absolute;
 left: 8px;
 right: 8px;
 top: 0;
 bottom: 0;
 pointer-events: none;
 border-radius: var(--n-border-radius);
 transition: background-color .3s var(--n-bezier);
 `),_("disabled",`
 opacity: .45;
 cursor: not-allowed;
 `),_("collapsed",[m("arrow","transform: rotate(0);")]),_("selected",[g("&::before","background-color: var(--n-item-color-active);"),m("arrow","color: var(--n-arrow-color-active);"),m("icon","color: var(--n-item-icon-color-active);"),d("menu-item-content-header",`
 color: var(--n-item-text-color-active);
 `,[g("a","color: var(--n-item-text-color-active);"),m("extra","color: var(--n-item-text-color-active);")])]),_("child-active",[d("menu-item-content-header",`
 color: var(--n-item-text-color-child-active);
 `,[g("a",`
 color: var(--n-item-text-color-child-active);
 `),m("extra",`
 color: var(--n-item-text-color-child-active);
 `)]),m("arrow",`
 color: var(--n-arrow-color-child-active);
 `),m("icon",`
 color: var(--n-item-icon-color-child-active);
 `)]),oe("disabled",[oe("selected, child-active",[g("&:focus-within",Pe)]),_("selected",[X(null,[m("arrow","color: var(--n-arrow-color-active-hover);"),m("icon","color: var(--n-item-icon-color-active-hover);"),d("menu-item-content-header",`
 color: var(--n-item-text-color-active-hover);
 `,[g("a","color: var(--n-item-text-color-active-hover);"),m("extra","color: var(--n-item-text-color-active-hover);")])])]),_("child-active",[X(null,[m("arrow","color: var(--n-arrow-color-child-active-hover);"),m("icon","color: var(--n-item-icon-color-child-active-hover);"),d("menu-item-content-header",`
 color: var(--n-item-text-color-child-active-hover);
 `,[g("a","color: var(--n-item-text-color-child-active-hover);"),m("extra","color: var(--n-item-text-color-child-active-hover);")])])]),_("selected",[X(null,[g("&::before","background-color: var(--n-item-color-active-hover);")])]),X(null,Pe)]),m("icon",`
 grid-area: icon;
 color: var(--n-item-icon-color);
 transition:
 color .3s var(--n-bezier),
 font-size .3s var(--n-bezier),
 margin-right .3s var(--n-bezier);
 box-sizing: content-box;
 display: inline-flex;
 align-items: center;
 justify-content: center;
 `),m("arrow",`
 grid-area: arrow;
 font-size: 16px;
 color: var(--n-arrow-color);
 transform: rotate(180deg);
 opacity: 1;
 transition:
 color .3s var(--n-bezier),
 transform 0.2s var(--n-bezier),
 opacity 0.2s var(--n-bezier);
 `),d("menu-item-content-header",`
 grid-area: content;
 transition:
 color .3s var(--n-bezier),
 opacity .3s var(--n-bezier);
 opacity: 1;
 white-space: nowrap;
 color: var(--n-item-text-color);
 `,[g("a",`
 outline: none;
 text-decoration: none;
 transition: color .3s var(--n-bezier);
 color: var(--n-item-text-color);
 `,[g("&::before",`
 content: "";
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 `)]),m("extra",`
 font-size: .93em;
 color: var(--n-group-text-color);
 transition: color .3s var(--n-bezier);
 `)])]),d("submenu",`
 cursor: pointer;
 position: relative;
 margin-top: 6px;
 `,[d("menu-item-content",`
 height: var(--n-item-height);
 `),d("submenu-children",`
 overflow: hidden;
 padding: 0;
 `,[xo({duration:".2s"})])]),d("menu-item-group",[d("menu-item-group-title",`
 margin-top: 6px;
 color: var(--n-group-text-color);
 cursor: default;
 font-size: .93em;
 height: 36px;
 display: flex;
 align-items: center;
 transition:
 padding-left .3s var(--n-bezier),
 color .3s var(--n-bezier);
 `)])]),d("menu-tooltip",[g("a",`
 color: inherit;
 text-decoration: none;
 `)]),d("menu-divider",`
 transition: background-color .3s var(--n-bezier);
 background-color: var(--n-divider-color);
 height: 1px;
 margin: 6px 18px;
 `)]);function X(e,t){return[_("hover",e,t),g("&:hover",e,t)]}const Ue=P({name:"MenuOptionContent",props:{collapsed:Boolean,disabled:Boolean,title:[String,Function],icon:Function,extra:[String,Function],showArrow:Boolean,childActive:Boolean,hover:Boolean,paddingLeft:Number,selected:Boolean,maxIconSize:{type:Number,required:!0},activeIconSize:{type:Number,required:!0},iconMarginRight:{type:Number,required:!0},clsPrefix:{type:String,required:!0},onClick:Function,tmNode:{type:Object,required:!0},isEllipsisPlaceholder:Boolean},setup(e){const{props:t}=G(le);return{menuProps:t,style:C(()=>{const{paddingLeft:o}=e;return{paddingLeft:o&&`${o}px`}}),iconStyle:C(()=>{const{maxIconSize:o,activeIconSize:r,iconMarginRight:i}=e;return{width:`${o}px`,height:`${o}px`,fontSize:`${r}px`,marginRight:`${i}px`}})}},render(){const{clsPrefix:e,tmNode:t,menuProps:{renderIcon:o,renderLabel:r,renderExtra:i,expandIcon:l}}=this,u=o?o(t.rawNode):Q(this.icon);return s("div",{onClick:v=>{var c;(c=this.onClick)===null||c===void 0||c.call(this,v)},role:"none",class:[`${e}-menu-item-content`,{[`${e}-menu-item-content--selected`]:this.selected,[`${e}-menu-item-content--collapsed`]:this.collapsed,[`${e}-menu-item-content--child-active`]:this.childActive,[`${e}-menu-item-content--disabled`]:this.disabled,[`${e}-menu-item-content--hover`]:this.hover}],style:this.style},u&&s("div",{class:`${e}-menu-item-content__icon`,style:this.iconStyle,role:"none"},[u]),s("div",{class:`${e}-menu-item-content-header`,role:"none"},this.isEllipsisPlaceholder?this.title:r?r(t.rawNode):Q(this.title),this.extra||i?s("span",{class:`${e}-menu-item-content-header__extra`}," ",i?i(t.rawNode):Q(this.extra)):null),this.showArrow?s(Le,{ariaHidden:!0,class:`${e}-menu-item-content__arrow`,clsPrefix:e},{default:()=>l?l(t.rawNode):s(Lo,null)}):null)}}),se=8;function _e(e){const t=G(le),{props:o,mergedCollapsedRef:r}=t,i=G(Ye,null),l=G(we,null),u=C(()=>o.mode==="horizontal"),v=C(()=>u.value?o.dropdownPlacement:"tmNodes"in e?"right-start":"right"),c=C(()=>{var h;return Math.max((h=o.collapsedIconSize)!==null&&h!==void 0?h:o.iconSize,o.iconSize)}),f=C(()=>{var h;return!u.value&&e.root&&r.value&&(h=o.collapsedIconSize)!==null&&h!==void 0?h:o.iconSize}),x=C(()=>{if(u.value)return;const{collapsedWidth:h,indent:N,rootIndent:I}=o,{root:y,isGroup:z}=e,$=I===void 0?N:I;return y?r.value?h/2-c.value/2:$:l&&typeof l.paddingLeftRef.value=="number"?N/2+l.paddingLeftRef.value:i&&typeof i.paddingLeftRef.value=="number"?(z?N/2:N)+i.paddingLeftRef.value:0}),b=C(()=>{const{collapsedWidth:h,indent:N,rootIndent:I}=o,{value:y}=c,{root:z}=e;return u.value||!z||!r.value?se:(I===void 0?N:I)+y+se-(h+y)/2});return{dropdownPlacement:v,activeIconSize:f,maxIconSize:c,paddingLeft:x,iconMarginRight:b,NMenu:t,NSubmenu:i,NMenuOptionGroup:l}}const ze={internalKey:{type:[String,Number],required:!0},root:Boolean,isGroup:Boolean,level:{type:Number,required:!0},title:[String,Function],extra:[String,Function]},mt=P({name:"MenuDivider",setup(){const e=G(le),{mergedClsPrefixRef:t,isHorizontalRef:o}=e;return()=>o.value?null:s("div",{class:`${t.value}-menu-divider`})}}),We=Object.assign(Object.assign({},ze),{tmNode:{type:Object,required:!0},disabled:Boolean,icon:Function,onClick:Function}),ht=xe(We),ft=P({name:"MenuOption",props:We,setup(e){const t=_e(e),{NSubmenu:o,NMenu:r,NMenuOptionGroup:i}=t,{props:l,mergedClsPrefixRef:u,mergedCollapsedRef:v}=r,c=o?o.mergedDisabledRef:i?i.mergedDisabledRef:{value:!1},f=C(()=>c.value||e.disabled);function x(h){const{onClick:N}=e;N&&N(h)}function b(h){f.value||(r.doSelect(e.internalKey,e.tmNode.rawNode),x(h))}return{mergedClsPrefix:u,dropdownPlacement:t.dropdownPlacement,paddingLeft:t.paddingLeft,iconMarginRight:t.iconMarginRight,maxIconSize:t.maxIconSize,activeIconSize:t.activeIconSize,mergedTheme:r.mergedThemeRef,menuProps:l,dropdownEnabled:he(()=>e.root&&v.value&&l.mode!=="horizontal"&&!f.value),selected:he(()=>r.mergedValueRef.value===e.internalKey),mergedDisabled:f,handleClick:b}},render(){const{mergedClsPrefix:e,mergedTheme:t,tmNode:o,menuProps:{renderLabel:r,nodeProps:i}}=this,l=i==null?void 0:i(o.rawNode);return s("div",Object.assign({},l,{role:"menuitem",class:[`${e}-menu-item`,l==null?void 0:l.class]}),s(Bo,{theme:t.peers.Tooltip,themeOverrides:t.peerOverrides.Tooltip,trigger:"hover",placement:this.dropdownPlacement,disabled:!this.dropdownEnabled||this.title===void 0,internalExtraClass:["menu-tooltip"]},{default:()=>r?r(o.rawNode):Q(this.title),trigger:()=>s(Ue,{tmNode:o,clsPrefix:e,paddingLeft:this.paddingLeft,iconMarginRight:this.iconMarginRight,maxIconSize:this.maxIconSize,activeIconSize:this.activeIconSize,selected:this.selected,title:this.title,extra:this.extra,disabled:this.mergedDisabled,icon:this.icon,onClick:this.handleClick})}))}}),qe=Object.assign(Object.assign({},ze),{tmNode:{type:Object,required:!0},tmNodes:{type:Array,required:!0}}),pt=xe(qe),gt=P({name:"MenuOptionGroup",props:qe,setup(e){const t=_e(e),{NSubmenu:o}=t,r=C(()=>o!=null&&o.mergedDisabledRef.value?!0:e.tmNode.disabled);ee(we,{paddingLeftRef:t.paddingLeft,mergedDisabledRef:r});const{mergedClsPrefixRef:i,props:l}=G(le);return function(){const{value:u}=i,v=t.paddingLeft.value,{nodeProps:c}=l,f=c==null?void 0:c(e.tmNode.rawNode);return s("div",{class:`${u}-menu-item-group`,role:"group"},s("div",Object.assign({},f,{class:[`${u}-menu-item-group-title`,f==null?void 0:f.class],style:[(f==null?void 0:f.style)||"",v!==void 0?`padding-left: ${v}px;`:""]}),Q(e.title),e.extra?s(Co,null," ",Q(e.extra)):null),s("div",null,e.tmNodes.map(x=>Ie(x,l))))}}});function pe(e){return e.type==="divider"||e.type==="render"}function bt(e){return e.type==="divider"}function Ie(e,t){const{rawNode:o}=e,{show:r}=o;if(r===!1)return null;if(pe(o))return bt(o)?s(mt,Object.assign({key:e.key},o.props)):null;const{labelField:i}=t,{key:l,level:u,isGroup:v}=e,c=Object.assign(Object.assign({},o),{title:o.title||o[i],extra:o.titleExtra||o.extra,key:l,internalKey:l,level:u,root:u===0,isGroup:v});return e.children?e.isGroup?s(gt,ue(c,pt,{tmNode:e,tmNodes:e.children,key:l})):s(ge,ue(c,xt,{key:l,rawNodes:o[t.childrenField],tmNodes:e.children,tmNode:e})):s(ft,ue(c,ht,{key:l,tmNode:e}))}const Ge=Object.assign(Object.assign({},ze),{rawNodes:{type:Array,default:()=>[]},tmNodes:{type:Array,default:()=>[]},tmNode:{type:Object,required:!0},disabled:Boolean,icon:Function,onClick:Function,domId:String,virtualChildActive:{type:Boolean,default:void 0},isEllipsisPlaceholder:Boolean}),xt=xe(Ge),ge=P({name:"Submenu",props:Ge,setup(e){const t=_e(e),{NMenu:o,NSubmenu:r}=t,{props:i,mergedCollapsedRef:l,mergedThemeRef:u}=o,v=C(()=>{const{disabled:h}=e;return r!=null&&r.mergedDisabledRef.value||i.disabled?!0:h}),c=B(!1);ee(Ye,{paddingLeftRef:t.paddingLeft,mergedDisabledRef:v}),ee(we,null);function f(){const{onClick:h}=e;h&&h()}function x(){v.value||(l.value||o.toggleExpand(e.internalKey),f())}function b(h){c.value=h}return{menuProps:i,mergedTheme:u,doSelect:o.doSelect,inverted:o.invertedRef,isHorizontal:o.isHorizontalRef,mergedClsPrefix:o.mergedClsPrefixRef,maxIconSize:t.maxIconSize,activeIconSize:t.activeIconSize,iconMarginRight:t.iconMarginRight,dropdownPlacement:t.dropdownPlacement,dropdownShow:c,paddingLeft:t.paddingLeft,mergedDisabled:v,mergedValue:o.mergedValueRef,childActive:he(()=>{var h;return(h=e.virtualChildActive)!==null&&h!==void 0?h:o.activePathRef.value.includes(e.internalKey)}),collapsed:C(()=>i.mode==="horizontal"?!1:l.value?!0:!o.mergedExpandedKeysRef.value.includes(e.internalKey)),dropdownEnabled:C(()=>!v.value&&(i.mode==="horizontal"||l.value)),handlePopoverShowChange:b,handleClick:x}},render(){var e;const{mergedClsPrefix:t,menuProps:{renderIcon:o,renderLabel:r}}=this,i=()=>{const{isHorizontal:u,paddingLeft:v,collapsed:c,mergedDisabled:f,maxIconSize:x,activeIconSize:b,title:h,childActive:N,icon:I,handleClick:y,menuProps:{nodeProps:z},dropdownShow:$,iconMarginRight:L,tmNode:V,mergedClsPrefix:M,isEllipsisPlaceholder:H,extra:A}=this,R=z==null?void 0:z(V.rawNode);return s("div",Object.assign({},R,{class:[`${M}-menu-item`,R==null?void 0:R.class],role:"menuitem"}),s(Ue,{tmNode:V,paddingLeft:v,collapsed:c,disabled:f,iconMarginRight:L,maxIconSize:x,activeIconSize:b,title:h,extra:A,showArrow:!u,childActive:N,clsPrefix:M,icon:I,hover:$,onClick:y,isEllipsisPlaceholder:H}))},l=()=>s(Oe,null,{default:()=>{const{tmNodes:u,collapsed:v}=this;return v?null:s("div",{class:`${t}-submenu-children`,role:"menu"},u.map(c=>Ie(c,this.menuProps)))}});return this.root?s(Oo,Object.assign({size:"large",trigger:"hover"},(e=this.menuProps)===null||e===void 0?void 0:e.dropdownProps,{themeOverrides:this.mergedTheme.peerOverrides.Dropdown,theme:this.mergedTheme.peers.Dropdown,builtinThemeOverrides:{fontSizeLarge:"14px",optionIconSizeLarge:"18px"},value:this.mergedValue,disabled:!this.dropdownEnabled,placement:this.dropdownPlacement,keyField:this.menuProps.keyField,labelField:this.menuProps.labelField,childrenField:this.menuProps.childrenField,onUpdateShow:this.handlePopoverShowChange,options:this.rawNodes,onSelect:this.doSelect,inverted:this.inverted,renderIcon:o,renderLabel:r}),{default:()=>s("div",{class:`${t}-submenu`,role:"menu","aria-expanded":!this.collapsed,id:this.domId},i(),this.isHorizontal?null:l())}):s("div",{class:`${t}-submenu`,role:"menu","aria-expanded":!this.collapsed,id:this.domId},i(),l())}}),Ct=Object.assign(Object.assign({},Y.props),{options:{type:Array,default:()=>[]},collapsed:{type:Boolean,default:void 0},collapsedWidth:{type:Number,default:48},iconSize:{type:Number,default:20},collapsedIconSize:{type:Number,default:24},rootIndent:Number,indent:{type:Number,default:32},labelField:{type:String,default:"label"},keyField:{type:String,default:"key"},childrenField:{type:String,default:"children"},disabledField:{type:String,default:"disabled"},defaultExpandAll:Boolean,defaultExpandedKeys:Array,expandedKeys:Array,value:[String,Number],defaultValue:{type:[String,Number],default:null},mode:{type:String,default:"vertical"},watchProps:{type:Array,default:void 0},disabled:Boolean,show:{type:Boolean,default:!0},inverted:Boolean,"onUpdate:expandedKeys":[Function,Array],onUpdateExpandedKeys:[Function,Array],onUpdateValue:[Function,Array],"onUpdate:value":[Function,Array],expandIcon:Function,renderIcon:Function,renderLabel:Function,renderExtra:Function,dropdownProps:Object,accordion:Boolean,nodeProps:Function,dropdownPlacement:{type:String,default:"bottom"},responsive:Boolean,items:Array,onOpenNamesChange:[Function,Array],onSelect:[Function,Array],onExpandedNamesChange:[Function,Array],expandedNames:Array,defaultExpandedNames:Array}),yt=P({name:"Menu",inheritAttrs:!1,props:Ct,setup(e){const{mergedClsPrefixRef:t,inlineThemeDisabled:o}=te(e),r=Y("Menu","-menu",vt,Qo,e,t),i=G(Ve,null),l=C(()=>{var p;const{collapsed:S}=e;if(S!==void 0)return S;if(i){const{collapseModeRef:n,collapsedRef:w}=i;if(n.value==="width")return(p=w.value)!==null&&p!==void 0?p:!1}return!1}),u=C(()=>{const{keyField:p,childrenField:S,disabledField:n}=e;return me(e.items||e.options,{getIgnored(w){return pe(w)},getChildren(w){return w[S]},getDisabled(w){return w[n]},getKey(w){var T;return(T=w[p])!==null&&T!==void 0?T:w.name}})}),v=C(()=>new Set(u.value.treeNodes.map(p=>p.key))),{watchProps:c}=e,f=B(null);c!=null&&c.includes("defaultValue")?$e(()=>{f.value=e.defaultValue}):f.value=e.defaultValue;const x=Z(e,"value"),b=fe(x,f),h=B([]),N=()=>{h.value=e.defaultExpandAll?u.value.getNonLeafKeys():e.defaultExpandedNames||e.defaultExpandedKeys||u.value.getPath(b.value,{includeSelf:!1}).keyPath};c!=null&&c.includes("defaultExpandedKeys")?$e(N):N();const I=Mo(e,["expandedNames","expandedKeys"]),y=fe(I,h),z=C(()=>u.value.treeNodes),$=C(()=>u.value.getPath(b.value).keyPath);ee(le,{props:e,mergedCollapsedRef:l,mergedThemeRef:r,mergedValueRef:b,mergedExpandedKeysRef:y,activePathRef:$,mergedClsPrefixRef:t,isHorizontalRef:C(()=>e.mode==="horizontal"),invertedRef:Z(e,"inverted"),doSelect:L,toggleExpand:M});function L(p,S){const{"onUpdate:value":n,onUpdateValue:w,onSelect:T}=e;w&&D(w,p,S),n&&D(n,p,S),T&&D(T,p,S),f.value=p}function V(p){const{"onUpdate:expandedKeys":S,onUpdateExpandedKeys:n,onExpandedNamesChange:w,onOpenNamesChange:T}=e;S&&D(S,p),n&&D(n,p),w&&D(w,p),T&&D(T,p),h.value=p}function M(p){const S=Array.from(y.value),n=S.findIndex(w=>w===p);if(~n)S.splice(n,1);else{if(e.accordion&&v.value.has(p)){const w=S.findIndex(T=>v.value.has(T));w>-1&&S.splice(w,1)}S.push(p)}V(S)}const H=p=>{const S=u.value.getPath(p??b.value,{includeSelf:!1}).keyPath;if(!S.length)return;const n=Array.from(y.value),w=new Set([...n,...S]);e.accordion&&v.value.forEach(T=>{w.has(T)&&!S.includes(T)&&w.delete(T)}),V(Array.from(w))},A=C(()=>{const{inverted:p}=e,{common:{cubicBezierEaseInOut:S},self:n}=r.value,{borderRadius:w,borderColorHorizontal:T,fontSize:ro,itemHeight:no,dividerColor:lo}=n,a={"--n-divider-color":lo,"--n-bezier":S,"--n-font-size":ro,"--n-border-color-horizontal":T,"--n-border-radius":w,"--n-item-height":no};return p?(a["--n-group-text-color"]=n.groupTextColorInverted,a["--n-color"]=n.colorInverted,a["--n-item-text-color"]=n.itemTextColorInverted,a["--n-item-text-color-hover"]=n.itemTextColorHoverInverted,a["--n-item-text-color-active"]=n.itemTextColorActiveInverted,a["--n-item-text-color-child-active"]=n.itemTextColorChildActiveInverted,a["--n-item-text-color-child-active-hover"]=n.itemTextColorChildActiveInverted,a["--n-item-text-color-active-hover"]=n.itemTextColorActiveHoverInverted,a["--n-item-icon-color"]=n.itemIconColorInverted,a["--n-item-icon-color-hover"]=n.itemIconColorHoverInverted,a["--n-item-icon-color-active"]=n.itemIconColorActiveInverted,a["--n-item-icon-color-active-hover"]=n.itemIconColorActiveHoverInverted,a["--n-item-icon-color-child-active"]=n.itemIconColorChildActiveInverted,a["--n-item-icon-color-child-active-hover"]=n.itemIconColorChildActiveHoverInverted,a["--n-item-icon-color-collapsed"]=n.itemIconColorCollapsedInverted,a["--n-item-text-color-horizontal"]=n.itemTextColorHorizontalInverted,a["--n-item-text-color-hover-horizontal"]=n.itemTextColorHoverHorizontalInverted,a["--n-item-text-color-active-horizontal"]=n.itemTextColorActiveHorizontalInverted,a["--n-item-text-color-child-active-horizontal"]=n.itemTextColorChildActiveHorizontalInverted,a["--n-item-text-color-child-active-hover-horizontal"]=n.itemTextColorChildActiveHoverHorizontalInverted,a["--n-item-text-color-active-hover-horizontal"]=n.itemTextColorActiveHoverHorizontalInverted,a["--n-item-icon-color-horizontal"]=n.itemIconColorHorizontalInverted,a["--n-item-icon-color-hover-horizontal"]=n.itemIconColorHoverHorizontalInverted,a["--n-item-icon-color-active-horizontal"]=n.itemIconColorActiveHorizontalInverted,a["--n-item-icon-color-active-hover-horizontal"]=n.itemIconColorActiveHoverHorizontalInverted,a["--n-item-icon-color-child-active-horizontal"]=n.itemIconColorChildActiveHorizontalInverted,a["--n-item-icon-color-child-active-hover-horizontal"]=n.itemIconColorChildActiveHoverHorizontalInverted,a["--n-arrow-color"]=n.arrowColorInverted,a["--n-arrow-color-hover"]=n.arrowColorHoverInverted,a["--n-arrow-color-active"]=n.arrowColorActiveInverted,a["--n-arrow-color-active-hover"]=n.arrowColorActiveHoverInverted,a["--n-arrow-color-child-active"]=n.arrowColorChildActiveInverted,a["--n-arrow-color-child-active-hover"]=n.arrowColorChildActiveHoverInverted,a["--n-item-color-hover"]=n.itemColorHoverInverted,a["--n-item-color-active"]=n.itemColorActiveInverted,a["--n-item-color-active-hover"]=n.itemColorActiveHoverInverted,a["--n-item-color-active-collapsed"]=n.itemColorActiveCollapsedInverted):(a["--n-group-text-color"]=n.groupTextColor,a["--n-color"]=n.color,a["--n-item-text-color"]=n.itemTextColor,a["--n-item-text-color-hover"]=n.itemTextColorHover,a["--n-item-text-color-active"]=n.itemTextColorActive,a["--n-item-text-color-child-active"]=n.itemTextColorChildActive,a["--n-item-text-color-child-active-hover"]=n.itemTextColorChildActiveHover,a["--n-item-text-color-active-hover"]=n.itemTextColorActiveHover,a["--n-item-icon-color"]=n.itemIconColor,a["--n-item-icon-color-hover"]=n.itemIconColorHover,a["--n-item-icon-color-active"]=n.itemIconColorActive,a["--n-item-icon-color-active-hover"]=n.itemIconColorActiveHover,a["--n-item-icon-color-child-active"]=n.itemIconColorChildActive,a["--n-item-icon-color-child-active-hover"]=n.itemIconColorChildActiveHover,a["--n-item-icon-color-collapsed"]=n.itemIconColorCollapsed,a["--n-item-text-color-horizontal"]=n.itemTextColorHorizontal,a["--n-item-text-color-hover-horizontal"]=n.itemTextColorHoverHorizontal,a["--n-item-text-color-active-horizontal"]=n.itemTextColorActiveHorizontal,a["--n-item-text-color-child-active-horizontal"]=n.itemTextColorChildActiveHorizontal,a["--n-item-text-color-child-active-hover-horizontal"]=n.itemTextColorChildActiveHoverHorizontal,a["--n-item-text-color-active-hover-horizontal"]=n.itemTextColorActiveHoverHorizontal,a["--n-item-icon-color-horizontal"]=n.itemIconColorHorizontal,a["--n-item-icon-color-hover-horizontal"]=n.itemIconColorHoverHorizontal,a["--n-item-icon-color-active-horizontal"]=n.itemIconColorActiveHorizontal,a["--n-item-icon-color-active-hover-horizontal"]=n.itemIconColorActiveHoverHorizontal,a["--n-item-icon-color-child-active-horizontal"]=n.itemIconColorChildActiveHorizontal,a["--n-item-icon-color-child-active-hover-horizontal"]=n.itemIconColorChildActiveHoverHorizontal,a["--n-arrow-color"]=n.arrowColor,a["--n-arrow-color-hover"]=n.arrowColorHover,a["--n-arrow-color-active"]=n.arrowColorActive,a["--n-arrow-color-active-hover"]=n.arrowColorActiveHover,a["--n-arrow-color-child-active"]=n.arrowColorChildActive,a["--n-arrow-color-child-active-hover"]=n.arrowColorChildActiveHover,a["--n-item-color-hover"]=n.itemColorHover,a["--n-item-color-active"]=n.itemColorActive,a["--n-item-color-active-hover"]=n.itemColorActiveHover,a["--n-item-color-active-collapsed"]=n.itemColorActiveCollapsed),a}),R=o?re("menu",C(()=>e.inverted?"a":"b"),A,e):void 0,W=wo(),U=B(null),ce=B(null);let j=!0;const ke=()=>{var p;j?j=!1:(p=U.value)===null||p===void 0||p.sync({showAllItemsBeforeCalculate:!0})};function Xe(){return document.getElementById(W)}const ie=B(-1);function Ze(p){ie.value=e.options.length-p}function Je(p){p||(ie.value=-1)}const Qe=C(()=>{const p=ie.value;return{children:p===-1?[]:e.options.slice(p)}}),eo=C(()=>{const{childrenField:p,disabledField:S,keyField:n}=e;return me([Qe.value],{getIgnored(w){return pe(w)},getChildren(w){return w[p]},getDisabled(w){return w[S]},getKey(w){var T;return(T=w[n])!==null&&T!==void 0?T:w.name}})}),oo=C(()=>me([{}]).treeNodes[0]);function to(){var p;if(ie.value===-1)return s(ge,{root:!0,level:0,key:"__ellpisisGroupPlaceholder__",internalKey:"__ellpisisGroupPlaceholder__",title:"···",tmNode:oo.value,domId:W,isEllipsisPlaceholder:!0});const S=eo.value.treeNodes[0],n=$.value,w=!!(!((p=S.children)===null||p===void 0)&&p.some(T=>n.includes(T.key)));return s(ge,{level:0,root:!0,key:"__ellpisisGroup__",internalKey:"__ellpisisGroup__",title:"···",virtualChildActive:w,tmNode:S,domId:W,rawNodes:S.rawNode.children||[],tmNodes:S.children||[],isEllipsisPlaceholder:!0})}return{mergedClsPrefix:t,controlledExpandedKeys:I,uncontrolledExpanededKeys:h,mergedExpandedKeys:y,uncontrolledValue:f,mergedValue:b,activePath:$,tmNodes:z,mergedTheme:r,mergedCollapsed:l,cssVars:o?void 0:A,themeClass:R==null?void 0:R.themeClass,overflowRef:U,counterRef:ce,updateCounter:()=>{},onResize:ke,onUpdateOverflow:Je,onUpdateCount:Ze,renderCounter:to,getCounter:Xe,onRender:R==null?void 0:R.onRender,showOption:H,deriveResponsiveState:ke}},render(){const{mergedClsPrefix:e,mode:t,themeClass:o,onRender:r}=this;r==null||r();const i=()=>this.tmNodes.map(c=>Ie(c,this.$props)),u=t==="horizontal"&&this.responsive,v=()=>s("div",_o(this.$attrs,{role:t==="horizontal"?"menubar":"menu",class:[`${e}-menu`,o,`${e}-menu--${t}`,u&&`${e}-menu--responsive`,this.mergedCollapsed&&`${e}-menu--collapsed`],style:this.cssVars}),u?s(jo,{ref:"overflowRef",onUpdateOverflow:this.onUpdateOverflow,getCounter:this.getCounter,onUpdateCount:this.onUpdateCount,updateCounter:this.updateCounter,style:{width:"100%",display:"flex",overflow:"hidden"}},{default:i,counter:this.renderCounter}):i());return u?s(yo,{onResize:this.onResize},{default:v}):v()}}),wt={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},_t=k("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32",d:"M256 112v288"},null,-1),zt=k("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32",d:"M400 256H112"},null,-1),It=[_t,zt],kt=P({name:"AddOutline",render:function(t,o){return F(),K("svg",wt,It)}}),St={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},At=k("path",{d:"M256 80c-8.66 0-16.58 7.36-16 16l8 216a8 8 0 0 0 8 8h0a8 8 0 0 0 8-8l8-216c.58-8.64-7.34-16-16-16z",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),Rt=k("circle",{cx:"256",cy:"416",r:"16",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),$t=[At,Rt],Ht=P({name:"AlertOutline",render:function(t,o){return F(),K("svg",St,$t)}}),Pt={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},Nt=k("path",{d:"M32 32v432a16 16 0 0 0 16 16h432",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),Tt=k("rect",{x:"96",y:"224",width:"80",height:"192",rx:"20",ry:"20",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),Bt=k("rect",{x:"240",y:"176",width:"80",height:"240",rx:"20",ry:"20",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),Ot=k("rect",{x:"383.64",y:"112",width:"80",height:"304",rx:"20",ry:"20",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),Mt=[Nt,Tt,Bt,Ot],Te=P({name:"BarChartOutline",render:function(t,o){return F(),K("svg",Pt,Mt)}}),Et={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},jt=k("path",{d:"M440 432H72a40 40 0 0 1-40-40V120a40 40 0 0 1 40-40h75.89a40 40 0 0 1 22.19 6.72l27.84 18.56a40 40 0 0 0 22.19 6.72H440a40 40 0 0 1 40 40v240a40 40 0 0 1-40 40z",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),Ft=k("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32",d:"M32 192h448"},null,-1),Lt=[jt,Ft],Vt=P({name:"FolderOutline",render:function(t,o){return F(),K("svg",Et,Lt)}}),Kt={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},Dt=k("path",{d:"M80 212v236a16 16 0 0 0 16 16h96V328a24 24 0 0 1 24-24h80a24 24 0 0 1 24 24v136h96a16 16 0 0 0 16-16V212",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),Yt=k("path",{d:"M480 256L266.89 52c-5-5.28-16.69-5.34-21.78 0L32 256",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),Ut=k("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32",d:"M400 179V64h-48v69"},null,-1),Wt=[Dt,Yt,Ut],qt=P({name:"HomeOutline",render:function(t,o){return F(),K("svg",Kt,Wt)}}),Gt={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},Xt=k("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-miterlimit":"10","stroke-width":"32",d:"M80 160h352"},null,-1),Zt=k("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-miterlimit":"10","stroke-width":"32",d:"M80 256h352"},null,-1),Jt=k("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-miterlimit":"10","stroke-width":"32",d:"M80 352h352"},null,-1),Qt=[Xt,Zt,Jt],er=P({name:"MenuOutline",render:function(t,o){return F(),K("svg",Gt,Qt)}}),or={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},tr=k("path",{d:"M427.68 351.43C402 320 383.87 304 383.87 217.35C383.87 138 343.35 109.73 310 96c-4.43-1.82-8.6-6-9.95-10.55C294.2 65.54 277.8 48 256 48s-38.21 17.55-44 37.47c-1.35 4.6-5.52 8.71-9.95 10.53c-33.39 13.75-73.87 41.92-73.87 121.35C128.13 304 110 320 84.32 351.43C73.68 364.45 83 384 101.61 384h308.88c18.51 0 27.77-19.61 17.19-32.57z",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),rr=k("path",{d:"M320 384v16a64 64 0 0 1-128 0v-16",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),nr=[tr,rr],lr=P({name:"NotificationsOutline",render:function(t,o){return F(),K("svg",or,nr)}}),ir={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},ar=k("path",{fill:"none",stroke:"currentColor","stroke-linejoin":"round","stroke-width":"32",d:"M160 336V48l32 16l32-16l31.94 16l32.37-16L320 64l31.79-16l31.93 16L416 48l32.01 16L480 48v224"},null,-1),sr=k("path",{d:"M480 272v112a80 80 0 0 1-80 80h0a80 80 0 0 1-80-80v-48H48a15.86 15.86 0 0 0-16 16c0 64 6.74 112 80 112h288",fill:"none",stroke:"currentColor","stroke-linejoin":"round","stroke-width":"32"},null,-1),cr=k("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32",d:"M224 144h192"},null,-1),dr=k("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32",d:"M288 224h128"},null,-1),ur=[ar,sr,cr,dr],vr=P({name:"ReceiptOutline",render:function(t,o){return F(),K("svg",ir,ur)}}),mr={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},hr=k("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32",d:"M320 120l48 48l-48 48"},null,-1),fr=k("path",{d:"M352 168H144a80.24 80.24 0 0 0-80 80v16",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),pr=k("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32",d:"M192 392l-48-48l48-48"},null,-1),gr=k("path",{d:"M160 344h208a80.24 80.24 0 0 0 80-80v-16",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),br=[hr,fr,pr,gr],xr=P({name:"RepeatOutline",render:function(t,o){return F(),K("svg",mr,br)}}),Cr={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},yr=k("path",{d:"M262.29 192.31a64 64 0 1 0 57.4 57.4a64.13 64.13 0 0 0-57.4-57.4zM416.39 256a154.34 154.34 0 0 1-1.53 20.79l45.21 35.46a10.81 10.81 0 0 1 2.45 13.75l-42.77 74a10.81 10.81 0 0 1-13.14 4.59l-44.9-18.08a16.11 16.11 0 0 0-15.17 1.75A164.48 164.48 0 0 1 325 400.8a15.94 15.94 0 0 0-8.82 12.14l-6.73 47.89a11.08 11.08 0 0 1-10.68 9.17h-85.54a11.11 11.11 0 0 1-10.69-8.87l-6.72-47.82a16.07 16.07 0 0 0-9-12.22a155.3 155.3 0 0 1-21.46-12.57a16 16 0 0 0-15.11-1.71l-44.89 18.07a10.81 10.81 0 0 1-13.14-4.58l-42.77-74a10.8 10.8 0 0 1 2.45-13.75l38.21-30a16.05 16.05 0 0 0 6-14.08c-.36-4.17-.58-8.33-.58-12.5s.21-8.27.58-12.35a16 16 0 0 0-6.07-13.94l-38.19-30A10.81 10.81 0 0 1 49.48 186l42.77-74a10.81 10.81 0 0 1 13.14-4.59l44.9 18.08a16.11 16.11 0 0 0 15.17-1.75A164.48 164.48 0 0 1 187 111.2a15.94 15.94 0 0 0 8.82-12.14l6.73-47.89A11.08 11.08 0 0 1 213.23 42h85.54a11.11 11.11 0 0 1 10.69 8.87l6.72 47.82a16.07 16.07 0 0 0 9 12.22a155.3 155.3 0 0 1 21.46 12.57a16 16 0 0 0 15.11 1.71l44.89-18.07a10.81 10.81 0 0 1 13.14 4.58l42.77 74a10.8 10.8 0 0 1-2.45 13.75l-38.21 30a16.05 16.05 0 0 0-6.05 14.08c.33 4.14.55 8.3.55 12.47z",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),wr=[yr],_r=P({name:"SettingsOutline",render:function(t,o){return F(),K("svg",Cr,wr)}}),zr={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},Ir=k("rect",{x:"48",y:"144",width:"416",height:"288",rx:"48",ry:"48",fill:"none",stroke:"currentColor","stroke-linejoin":"round","stroke-width":"32"},null,-1),kr=k("path",{d:"M411.36 144v-30A50 50 0 0 0 352 64.9L88.64 109.85A50 50 0 0 0 48 159v49",fill:"none",stroke:"currentColor","stroke-linejoin":"round","stroke-width":"32"},null,-1),Sr=k("path",{d:"M368 320a32 32 0 1 1 32-32a32 32 0 0 1-32 32z",fill:"currentColor"},null,-1),Ar=[Ir,kr,Sr],Rr=P({name:"WalletOutline",render:function(t,o){return F(),K("svg",zr,Ar)}}),$r={class:"logo"},Hr={key:0},Pr={style:{"font-size":"16px"}},Nr={__name:"Layout",setup(e){const t=Io(),o=ko(),r=B(!1);Me(()=>{window.innerWidth<768&&(r.value=!0)});const i=x=>()=>s(ae,null,{default:()=>s(x)}),l=[{label:"首页",key:"Dashboard",icon:i(qt)},{label:"交易",key:"Transactions",icon:i(vr)},{label:"账户",key:"Accounts",icon:i(Rr)},{label:"分类",key:"Categories",icon:i(Vt)},{label:"预算",key:"Budget",icon:i(Te)},{label:"周期",key:"Recurring",icon:i(xr)},{label:"预警",key:"AlertRules",icon:i(Ht)},{label:"报表",key:"Reports",icon:i(Te)},{label:"设置",key:"Settings",icon:i(_r)}],u=C(()=>o.name||"Dashboard"),v=B(0),c=C(()=>({Dashboard:"首页",Transactions:"交易记录",AddTransaction:"新增交易",EditTransaction:"编辑交易",Accounts:"账户管理",Categories:"分类管理",Budget:"预算管理",Recurring:"周期交易",AlertRules:"预警规则",Reports:"统计报表",Settings:"系统设置"})[o.name]||"BudgetPilot"),f=x=>{t.push({name:x}),window.innerWidth<768&&(r.value=!0)};return(x,b)=>{const h=yt,N=ut,I=Ro,y=Fo,z=Go,$=it,L=So("router-view"),V=rt,M=tt;return F(),zo(M,{"has-sider":"",style:{height:"100vh"},"native-scrollbar":!1},{default:E(()=>[O(N,{bordered:"",collapsed:r.value,width:220,"collapsed-width":0,"show-trigger":"bar","collapse-mode":"transform",onCollapse:b[0]||(b[0]=H=>r.value=!0),onExpand:b[1]||(b[1]=H=>r.value=!1),"native-scrollbar":!1,class:"sider-wrapper"},{default:E(()=>[k("div",$r,[r.value?Ao("",!0):(F(),K("h2",Hr,"BudgetPilot"))]),O(h,{collapsed:r.value,"collapsed-width":0,"collapsed-icon-size":22,options:l,value:u.value,"onUpdate:value":f},null,8,["collapsed","value"])]),_:1},8,["collapsed"]),O(M,{style:{height:"100vh"},"native-scrollbar":!1},{default:E(()=>[O($,{bordered:"",style:{padding:"12px 16px",background:"#fff",display:"flex","justify-content":"space-between","align-items":"center"}},{default:E(()=>[O(y,{size:12,align:"center"},{default:E(()=>[O(I,{quaternary:"",circle:"",size:"small",onClick:b[2]||(b[2]=H=>r.value=!r.value),class:"menu-toggle"},{icon:E(()=>[O(q(ae),null,{default:E(()=>[O(q(er))]),_:1})]),_:1}),k("h3",Pr,$o(c.value),1)]),_:1}),O(y,{size:8},{default:E(()=>[O(z,{value:v.value,show:v.value>0},{default:E(()=>[O(I,{quaternary:"",circle:"",size:"small",onClick:b[3]||(b[3]=H=>q(t).push("/alerts"))},{icon:E(()=>[O(q(ae),null,{default:E(()=>[O(q(lr))]),_:1})]),_:1})]),_:1},8,["value","show"]),O(I,{type:"primary",size:"small",onClick:b[4]||(b[4]=H=>q(t).push("/transactions/add")),class:"add-btn"},{icon:E(()=>[O(q(ae),null,{default:E(()=>[O(q(kt))]),_:1})]),default:E(()=>[b[5]||(b[5]=k("span",{class:"add-text"},"记一笔",-1))]),_:1})]),_:1})]),_:1}),O(V,{"content-style":"padding: 16px;"},{default:E(()=>[O(L)]),_:1})]),_:1})]),_:1})}}},Vr=Ho(Nr,[["__scopeId","data-v-6a0bde57"]]);export{Vr as default};
