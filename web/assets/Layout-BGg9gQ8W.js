import{d as P,h as s,r as B,w as Be,n as Se,t as Z,c as C,a as g,b as lo,e as d,f as ao,g as z,i as m,u as so,T as co,N as Oe,j as be,k as Re,l as uo,m as vo,o as mo,p as te,q as Y,s as Me,v as ho,x as re,y as fo,z as po,A as go,B as Ee,C as bo,D as Ae,E as de,F as ne,S as je,G as Fe,H as ee,I as Le,J as G,K,L as oe,M as xo,O as Q,P as he,Q as xe,R as Co,U as ue,V as yo,W as $e,X as wo,Y as zo,Z as V,_ as D,$ as S,a0 as _o,a1 as E,a2 as Io,a3 as ko,a4 as So,a5 as O,a6 as Ro,a7 as Ao,a8 as q,a9 as $o}from"./index-D-6FuWe0.js";import{_ as Ho}from"./_plugin-vue_export-helper-DlAUqK2U.js";import{d as Po,t as No,C as To,N as Bo,a as Oo}from"./Dropdown-zC8djbUR.js";import{f as ve,u as fe,a as Mo}from"./get-TxSU5CLd.js";import{g as Eo,V as jo,c as me}from"./Popover-ByijyN-d.js";import{_ as Fo}from"./Space-Bh_03kZK.js";import{N as ae}from"./Icon-ADgloaJ5.js";import"./use-keyboard-C7sgregc.js";const Lo=P({name:"ChevronDownFilled",render(){return s("svg",{viewBox:"0 0 16 16",fill:"none",xmlns:"http://www.w3.org/2000/svg"},s("path",{d:"M3.20041 5.73966C3.48226 5.43613 3.95681 5.41856 4.26034 5.70041L8 9.22652L11.7397 5.70041C12.0432 5.41856 12.5177 5.43613 12.7996 5.73966C13.0815 6.0432 13.0639 6.51775 12.7603 6.7996L8.51034 10.7996C8.22258 11.0668 7.77743 11.0668 7.48967 10.7996L3.23966 6.7996C2.93613 6.51775 2.91856 6.0432 3.20041 5.73966Z",fill:"currentColor"}))}}),He=P({name:"SlotMachineNumber",props:{clsPrefix:{type:String,required:!0},value:{type:[Number,String],required:!0},oldOriginalNumber:{type:Number,default:void 0},newOriginalNumber:{type:Number,default:void 0}},setup(e){const t=B(null),o=B(e.value),r=B(e.value),l=B("up"),i=B(!1),u=C(()=>i.value?`${e.clsPrefix}-base-slot-machine-current-number--${l.value}-scroll`:null),v=C(()=>i.value?`${e.clsPrefix}-base-slot-machine-old-number--${l.value}-scroll`:null);Be(Z(e,"value"),(x,b)=>{o.value=b,r.value=x,Se(c)});function c(){const x=e.newOriginalNumber,b=e.oldOriginalNumber;b===void 0||x===void 0||(x>b?f("up"):b>x&&f("down"))}function f(x){l.value=x,i.value=!1,Se(()=>{var b;(b=t.value)===null||b===void 0||b.offsetWidth,i.value=!0})}return()=>{const{clsPrefix:x}=e;return s("span",{ref:t,class:`${x}-base-slot-machine-number`},o.value!==null?s("span",{class:[`${x}-base-slot-machine-old-number ${x}-base-slot-machine-old-number--top`,v.value]},o.value):null,s("span",{class:[`${x}-base-slot-machine-current-number`,u.value]},s("span",{ref:"numberWrapper",class:[`${x}-base-slot-machine-current-number__inner`,typeof e.value!="number"&&`${x}-base-slot-machine-current-number__inner--not-number`]},r.value)),o.value!==null?s("span",{class:[`${x}-base-slot-machine-old-number ${x}-base-slot-machine-old-number--bottom`,v.value]},o.value):null)}}}),{cubicBezierEaseOut:J}=lo;function Vo({duration:e=".2s"}={}){return[g("&.fade-up-width-expand-transition-leave-active",{transition:`
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
 `,[z("top",{transform:"translateY(-100%)"}),z("bottom",{transform:"translateY(100%)"}),z("down-scroll",{animation:"n-base-slot-machine-fade-down-out .2s cubic-bezier(0, 0, .2, 1)",animationIterationCount:1}),z("up-scroll",{animation:"n-base-slot-machine-fade-up-out .2s cubic-bezier(0, 0, .2, 1)",animationIterationCount:1})]),d("base-slot-machine-current-number",`
 display: inline-block;
 position: absolute;
 left: 0;
 top: 0;
 bottom: 0;
 right: 0;
 opacity: 1;
 transform: translateY(0);
 width: .6em;
 `,[z("down-scroll",{animation:"n-base-slot-machine-fade-down-in .2s cubic-bezier(0, 0, .2, 1)",animationIterationCount:1}),z("up-scroll",{animation:"n-base-slot-machine-fade-up-in .2s cubic-bezier(0, 0, .2, 1)",animationIterationCount:1}),m("inner",`
 display: inline-block;
 position: absolute;
 right: 0;
 top: 0;
 width: .6em;
 `,[z("not-number",`
 right: unset;
 left: 0;
 `)])])])])]),Do=P({name:"BaseSlotMachine",props:{clsPrefix:{type:String,required:!0},value:{type:[Number,String],default:0},max:{type:Number,default:void 0},appeared:{type:Boolean,required:!0}},setup(e){so("-base-slot-machine",Ko,Z(e,"clsPrefix"));const t=B(),o=B(),r=C(()=>{if(typeof e.value=="string")return[];if(e.value<1)return[0];const l=[];let i=e.value;for(e.max!==void 0&&(i=Math.min(e.max,i));i>=1;)l.push(i%10),i/=10,i=Math.floor(i);return l.reverse(),l});return Be(Z(e,"value"),(l,i)=>{typeof l=="string"?(o.value=void 0,t.value=void 0):typeof i=="string"?(o.value=l,t.value=void 0):(o.value=l,t.value=i)}),()=>{const{value:l,clsPrefix:i}=e;return typeof l=="number"?s("span",{class:`${i}-base-slot-machine`},s(co,{name:"fade-up-width-expand-transition",tag:"span"},{default:()=>r.value.map((u,v)=>s(He,{clsPrefix:i,key:r.value.length-v-1,oldOriginalNumber:t.value,newOriginalNumber:o.value,value:u}))}),s(Oe,{key:"+",width:!0},{default:()=>e.max!==void 0&&e.max<l?s(He,{clsPrefix:i,value:"+"}):null})):s("span",{class:`${i}-base-slot-machine`},l)}}});function Yo(e){const{errorColor:t,infoColor:o,successColor:r,warningColor:l,fontFamily:i}=e;return{color:t,colorInfo:o,colorSuccess:r,colorError:t,colorWarning:l,fontSize:"12px",fontFamily:i}}const Uo={common:be,self:Yo},Wo=g([g("@keyframes badge-wave-spread",{from:{boxShadow:"0 0 0.5px 0px var(--n-ripple-color)",opacity:.6},to:{boxShadow:"0 0 0.5px 4.5px var(--n-ripple-color)",opacity:0}}),d("badge",`
 display: inline-flex;
 position: relative;
 vertical-align: middle;
 font-family: var(--n-font-family);
 `,[z("as-is",[d("badge-sup",{position:"static",transform:"translateX(0)"},[Re({transformOrigin:"left bottom",originalTransform:"translateX(0)"})])]),z("dot",[d("badge-sup",`
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
 `,[Re({transformOrigin:"left bottom",originalTransform:"translateX(-50%)"}),d("base-wave",{zIndex:1,animationDuration:"2s",animationIterationCount:"infinite",animationDelay:"1s",animationTimingFunction:"var(--n-ripple-bezier)",animationName:"badge-wave-spread"}),g("&::before",`
 opacity: 0;
 transform: scale(1);
 border-radius: 9px;
 content: "";
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 `)])])]),qo=Object.assign(Object.assign({},Y.props),{value:[String,Number],max:Number,dot:Boolean,type:{type:String,default:"default"},show:{type:Boolean,default:!0},showZero:Boolean,processing:Boolean,color:String,offset:Array}),Go=P({name:"Badge",props:qo,setup(e,{slots:t}){const{mergedClsPrefixRef:o,inlineThemeDisabled:r,mergedRtlRef:l}=te(e),i=Y("Badge","-badge",Wo,Uo,e,o),u=B(!1),v=()=>{u.value=!0},c=()=>{u.value=!1},f=C(()=>e.show&&(e.dot||e.value!==void 0&&!(!e.showZero&&Number(e.value)<=0)||!fo(t.value)));Me(()=>{f.value&&(u.value=!0)});const x=ho("Badge",l,o),b=C(()=>{const{type:I,color:y}=e,{common:{cubicBezierEaseInOut:_,cubicBezierEaseOut:$},self:{[po("color",I)]:F,fontFamily:L,fontSize:M}}=i.value;return{"--n-font-size":M,"--n-font-family":L,"--n-color":y||F,"--n-ripple-color":y||F,"--n-bezier":_,"--n-ripple-bezier":$}}),h=r?re("badge",C(()=>{let I="";const{type:y,color:_}=e;return y&&(I+=y[0]),_&&(I+=go(_)),I}),b,e):void 0,N=C(()=>{const{offset:I}=e;if(!I)return;const[y,_]=I,$=typeof y=="number"?`${y}px`:y,F=typeof _=="number"?`${_}px`:_;return{transform:`translate(calc(${x!=null&&x.value?"50%":"-50%"} + ${$}), ${F})`}});return{rtlEnabled:x,mergedClsPrefix:o,appeared:u,showBadge:f,handleAfterEnter:v,handleAfterLeave:c,cssVars:r?void 0:b,themeClass:h==null?void 0:h.themeClass,onRender:h==null?void 0:h.onRender,offsetStyle:N}},render(){var e;const{mergedClsPrefix:t,onRender:o,themeClass:r,$slots:l}=this;o==null||o();const i=(e=l.default)===null||e===void 0?void 0:e.call(l);return s("div",{class:[`${t}-badge`,this.rtlEnabled&&`${t}-badge--rtl`,r,{[`${t}-badge--dot`]:this.dot,[`${t}-badge--as-is`]:!i}],style:this.cssVars},i,s(uo,{name:"fade-in-scale-up-transition",onAfterEnter:this.handleAfterEnter,onAfterLeave:this.handleAfterLeave},{default:()=>this.showBadge?s("sup",{class:`${t}-badge-sup`,title:Eo(this.value),style:this.offsetStyle},vo(l.value,()=>[this.dot?null:s(Do,{clsPrefix:t,appeared:this.appeared,max:this.max,value:this.value})]),this.processing?s(mo,{clsPrefix:t}):null):null}))}});function Xo(e){const{baseColor:t,textColor2:o,bodyColor:r,cardColor:l,dividerColor:i,actionColor:u,scrollbarColor:v,scrollbarColorHover:c,invertedColor:f}=e;return{textColor:o,textColorInverted:"#FFF",color:r,colorEmbedded:u,headerColor:l,headerColorInverted:f,footerColor:u,footerColorInverted:f,headerBorderColor:i,headerBorderColorInverted:f,footerBorderColor:i,footerBorderColorInverted:f,siderBorderColor:i,siderBorderColorInverted:f,siderColor:l,siderColorInverted:f,siderToggleButtonBorder:`1px solid ${i}`,siderToggleButtonColor:t,siderToggleButtonIconColor:o,siderToggleButtonIconColorInverted:o,siderToggleBarColor:Ae(r,v),siderToggleBarColorHover:Ae(r,c),__invertScrollbar:"true"}}const Ce=Ee({name:"Layout",common:be,peers:{Scrollbar:bo},self:Xo});function Zo(e,t,o,r){return{itemColorHoverInverted:"#0000",itemColorActiveInverted:t,itemColorActiveHoverInverted:t,itemColorActiveCollapsedInverted:t,itemTextColorInverted:e,itemTextColorHoverInverted:o,itemTextColorChildActiveInverted:o,itemTextColorChildActiveHoverInverted:o,itemTextColorActiveInverted:o,itemTextColorActiveHoverInverted:o,itemTextColorHorizontalInverted:e,itemTextColorHoverHorizontalInverted:o,itemTextColorChildActiveHorizontalInverted:o,itemTextColorChildActiveHoverHorizontalInverted:o,itemTextColorActiveHorizontalInverted:o,itemTextColorActiveHoverHorizontalInverted:o,itemIconColorInverted:e,itemIconColorHoverInverted:o,itemIconColorActiveInverted:o,itemIconColorActiveHoverInverted:o,itemIconColorChildActiveInverted:o,itemIconColorChildActiveHoverInverted:o,itemIconColorCollapsedInverted:e,itemIconColorHorizontalInverted:e,itemIconColorHoverHorizontalInverted:o,itemIconColorActiveHorizontalInverted:o,itemIconColorActiveHoverHorizontalInverted:o,itemIconColorChildActiveHorizontalInverted:o,itemIconColorChildActiveHoverHorizontalInverted:o,arrowColorInverted:e,arrowColorHoverInverted:o,arrowColorActiveInverted:o,arrowColorActiveHoverInverted:o,arrowColorChildActiveInverted:o,arrowColorChildActiveHoverInverted:o,groupTextColorInverted:r}}function Jo(e){const{borderRadius:t,textColor3:o,primaryColor:r,textColor2:l,textColor1:i,fontSize:u,dividerColor:v,hoverColor:c,primaryColorHover:f}=e;return Object.assign({borderRadius:t,color:"#0000",groupTextColor:o,itemColorHover:c,itemColorActive:de(r,{alpha:.1}),itemColorActiveHover:de(r,{alpha:.1}),itemColorActiveCollapsed:de(r,{alpha:.1}),itemTextColor:l,itemTextColorHover:l,itemTextColorActive:r,itemTextColorActiveHover:r,itemTextColorChildActive:r,itemTextColorChildActiveHover:r,itemTextColorHorizontal:l,itemTextColorHoverHorizontal:f,itemTextColorActiveHorizontal:r,itemTextColorActiveHoverHorizontal:r,itemTextColorChildActiveHorizontal:r,itemTextColorChildActiveHoverHorizontal:r,itemIconColor:i,itemIconColorHover:i,itemIconColorActive:r,itemIconColorActiveHover:r,itemIconColorChildActive:r,itemIconColorChildActiveHover:r,itemIconColorCollapsed:i,itemIconColorHorizontal:i,itemIconColorHoverHorizontal:f,itemIconColorActiveHorizontal:r,itemIconColorActiveHoverHorizontal:r,itemIconColorChildActiveHorizontal:r,itemIconColorChildActiveHoverHorizontal:r,itemHeight:"42px",arrowColor:l,arrowColorHover:l,arrowColorActive:r,arrowColorActiveHover:r,arrowColorChildActive:r,arrowColorChildActiveHover:r,colorInverted:"#0000",borderColorHorizontal:"#0000",fontSize:u,dividerColor:v},Zo("#BBB",r,"#FFF","#AAA"))}const Qo=Ee({name:"Menu",common:be,peers:{Tooltip:No,Dropdown:Po},self:Jo}),Ve=ne("n-layout-sider"),ye={type:String,default:"static"},et=d("layout",`
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
 `),z("absolute-positioned",`
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 `)]),ot={embedded:Boolean,position:ye,nativeScrollbar:{type:Boolean,default:!0},scrollbarProps:Object,onScroll:Function,contentClass:String,contentStyle:{type:[String,Object],default:""},hasSider:Boolean,siderPlacement:{type:String,default:"left"}},Ke=ne("n-layout");function De(e){return P({name:e?"LayoutContent":"Layout",props:Object.assign(Object.assign({},Y.props),ot),setup(t){const o=B(null),r=B(null),{mergedClsPrefixRef:l,inlineThemeDisabled:i}=te(t),u=Y("Layout","-layout",et,Ce,t,l);function v(y,_){if(t.nativeScrollbar){const{value:$}=o;$&&(_===void 0?$.scrollTo(y):$.scrollTo(y,_))}else{const{value:$}=r;$&&$.scrollTo(y,_)}}ee(Ke,t);let c=0,f=0;const x=y=>{var _;const $=y.target;c=$.scrollLeft,f=$.scrollTop,(_=t.onScroll)===null||_===void 0||_.call(t,y)};Fe(()=>{if(t.nativeScrollbar){const y=o.value;y&&(y.scrollTop=f,y.scrollLeft=c)}});const b={display:"flex",flexWrap:"nowrap",width:"100%",flexDirection:"row"},h={scrollTo:v},N=C(()=>{const{common:{cubicBezierEaseInOut:y},self:_}=u.value;return{"--n-bezier":y,"--n-color":t.embedded?_.colorEmbedded:_.color,"--n-text-color":_.textColor}}),I=i?re("layout",C(()=>t.embedded?"e":""),N,t):void 0;return Object.assign({mergedClsPrefix:l,scrollableElRef:o,scrollbarInstRef:r,hasSiderStyle:b,mergedTheme:u,handleNativeElScroll:x,cssVars:i?void 0:N,themeClass:I==null?void 0:I.themeClass,onRender:I==null?void 0:I.onRender},h)},render(){var t;const{mergedClsPrefix:o,hasSider:r}=this;(t=this.onRender)===null||t===void 0||t.call(this);const l=r?this.hasSiderStyle:void 0,i=[this.themeClass,e&&`${o}-layout-content`,`${o}-layout`,`${o}-layout--${this.position}-positioned`];return s("div",{class:i,style:this.cssVars},this.nativeScrollbar?s("div",{ref:"scrollableElRef",class:[`${o}-layout-scroll-container`,this.contentClass],style:[this.contentStyle,l],onScroll:this.handleNativeElScroll},this.$slots):s(je,Object.assign({},this.scrollbarProps,{onScroll:this.onScroll,ref:"scrollbarInstRef",theme:this.mergedTheme.peers.Scrollbar,themeOverrides:this.mergedTheme.peerOverrides.Scrollbar,contentClass:this.contentClass,contentStyle:[this.contentStyle,l]}),this.$slots))}})}const tt=De(!1),rt=De(!0),nt=d("layout-header",`
 transition:
 color .3s var(--n-bezier),
 background-color .3s var(--n-bezier),
 box-shadow .3s var(--n-bezier),
 border-color .3s var(--n-bezier);
 box-sizing: border-box;
 width: 100%;
 background-color: var(--n-color);
 color: var(--n-text-color);
`,[z("absolute-positioned",`
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 `),z("bordered",`
 border-bottom: solid 1px var(--n-border-color);
 `)]),it={position:ye,inverted:Boolean,bordered:{type:Boolean,default:!1}},lt=P({name:"LayoutHeader",props:Object.assign(Object.assign({},Y.props),it),setup(e){const{mergedClsPrefixRef:t,inlineThemeDisabled:o}=te(e),r=Y("Layout","-layout-header",nt,Ce,e,t),l=C(()=>{const{common:{cubicBezierEaseInOut:u},self:v}=r.value,c={"--n-bezier":u};return e.inverted?(c["--n-color"]=v.headerColorInverted,c["--n-text-color"]=v.textColorInverted,c["--n-border-color"]=v.headerBorderColorInverted):(c["--n-color"]=v.headerColor,c["--n-text-color"]=v.textColor,c["--n-border-color"]=v.headerBorderColor),c}),i=o?re("layout-header",C(()=>e.inverted?"a":"b"),l,e):void 0;return{mergedClsPrefix:t,cssVars:o?void 0:l,themeClass:i==null?void 0:i.themeClass,onRender:i==null?void 0:i.onRender}},render(){var e;const{mergedClsPrefix:t}=this;return(e=this.onRender)===null||e===void 0||e.call(this),s("div",{class:[`${t}-layout-header`,this.themeClass,this.position&&`${t}-layout-header--${this.position}-positioned`,this.bordered&&`${t}-layout-header--bordered`],style:this.cssVars},this.$slots)}}),at=d("layout-sider",`
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
`,[z("bordered",[m("border",`
 content: "";
 position: absolute;
 top: 0;
 bottom: 0;
 width: 1px;
 background-color: var(--n-border-color);
 transition: background-color .3s var(--n-bezier);
 `)]),m("left-placement",[z("bordered",[m("border",`
 right: 0;
 `)])]),z("right-placement",`
 justify-content: flex-start;
 `,[z("bordered",[m("border",`
 left: 0;
 `)]),z("collapsed",[d("layout-toggle-button",[d("base-icon",`
 transform: rotate(180deg);
 `)]),d("layout-toggle-bar",[g("&:hover",[m("top",{transform:"rotate(-12deg) scale(1.15) translateY(-2px)"}),m("bottom",{transform:"rotate(12deg) scale(1.15) translateY(2px)"})])])]),d("layout-toggle-button",`
 left: 0;
 transform: translateX(-50%) translateY(-50%);
 `,[d("base-icon",`
 transform: rotate(0);
 `)]),d("layout-toggle-bar",`
 left: -28px;
 transform: rotate(180deg);
 `,[g("&:hover",[m("top",{transform:"rotate(12deg) scale(1.15) translateY(-2px)"}),m("bottom",{transform:"rotate(-12deg) scale(1.15) translateY(2px)"})])])]),z("collapsed",[d("layout-toggle-bar",[g("&:hover",[m("top",{transform:"rotate(-12deg) scale(1.15) translateY(-2px)"}),m("bottom",{transform:"rotate(12deg) scale(1.15) translateY(2px)"})])]),d("layout-toggle-button",[d("base-icon",`
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
 `),z("show-content",[d("layout-sider-scroll-container",{opacity:1})]),z("absolute-positioned",`
 position: absolute;
 left: 0;
 top: 0;
 bottom: 0;
 `)]),st=P({props:{clsPrefix:{type:String,required:!0},onClick:Function},render(){const{clsPrefix:e}=this;return s("div",{onClick:this.onClick,class:`${e}-layout-toggle-bar`},s("div",{class:`${e}-layout-toggle-bar__top`}),s("div",{class:`${e}-layout-toggle-bar__bottom`}))}}),ct=P({name:"LayoutToggleButton",props:{clsPrefix:{type:String,required:!0},onClick:Function},render(){const{clsPrefix:e}=this;return s("div",{class:`${e}-layout-toggle-button`,onClick:this.onClick},s(Le,{clsPrefix:e},{default:()=>s(To,null)}))}}),dt={position:ye,bordered:Boolean,collapsedWidth:{type:Number,default:48},width:{type:[Number,String],default:272},contentClass:String,contentStyle:{type:[String,Object],default:""},collapseMode:{type:String,default:"transform"},collapsed:{type:Boolean,default:void 0},defaultCollapsed:Boolean,showCollapsedContent:{type:Boolean,default:!0},showTrigger:{type:[Boolean,String],default:!1},nativeScrollbar:{type:Boolean,default:!0},inverted:Boolean,scrollbarProps:Object,triggerClass:String,triggerStyle:[String,Object],collapsedTriggerClass:String,collapsedTriggerStyle:[String,Object],"onUpdate:collapsed":[Function,Array],onUpdateCollapsed:[Function,Array],onAfterEnter:Function,onAfterLeave:Function,onExpand:[Function,Array],onCollapse:[Function,Array],onScroll:Function},ut=P({name:"LayoutSider",props:Object.assign(Object.assign({},Y.props),dt),setup(e){const t=G(Ke),o=B(null),r=B(null),l=B(e.defaultCollapsed),i=fe(Z(e,"collapsed"),l),u=C(()=>ve(i.value?e.collapsedWidth:e.width)),v=C(()=>e.collapseMode!=="transform"?{}:{minWidth:ve(e.width)}),c=C(()=>t?t.siderPlacement:"left");function f(H,R){if(e.nativeScrollbar){const{value:A}=o;A&&(R===void 0?A.scrollTo(H):A.scrollTo(H,R))}else{const{value:A}=r;A&&A.scrollTo(H,R)}}function x(){const{"onUpdate:collapsed":H,onUpdateCollapsed:R,onExpand:A,onCollapse:W}=e,{value:U}=i;R&&K(R,!U),H&&K(H,!U),l.value=!U,U?A&&K(A):W&&K(W)}let b=0,h=0;const N=H=>{var R;const A=H.target;b=A.scrollLeft,h=A.scrollTop,(R=e.onScroll)===null||R===void 0||R.call(e,H)};Fe(()=>{if(e.nativeScrollbar){const H=o.value;H&&(H.scrollTop=h,H.scrollLeft=b)}}),ee(Ve,{collapsedRef:i,collapseModeRef:Z(e,"collapseMode")});const{mergedClsPrefixRef:I,inlineThemeDisabled:y}=te(e),_=Y("Layout","-layout-sider",at,Ce,e,I);function $(H){var R,A;H.propertyName==="max-width"&&(i.value?(R=e.onAfterLeave)===null||R===void 0||R.call(e):(A=e.onAfterEnter)===null||A===void 0||A.call(e))}const F={scrollTo:f},L=C(()=>{const{common:{cubicBezierEaseInOut:H},self:R}=_.value,{siderToggleButtonColor:A,siderToggleButtonBorder:W,siderToggleBarColor:U,siderToggleBarColorHover:ce}=R,j={"--n-bezier":H,"--n-toggle-button-color":A,"--n-toggle-button-border":W,"--n-toggle-bar-color":U,"--n-toggle-bar-color-hover":ce};return e.inverted?(j["--n-color"]=R.siderColorInverted,j["--n-text-color"]=R.textColorInverted,j["--n-border-color"]=R.siderBorderColorInverted,j["--n-toggle-button-icon-color"]=R.siderToggleButtonIconColorInverted,j.__invertScrollbar=R.__invertScrollbar):(j["--n-color"]=R.siderColor,j["--n-text-color"]=R.textColor,j["--n-border-color"]=R.siderBorderColor,j["--n-toggle-button-icon-color"]=R.siderToggleButtonIconColor),j}),M=y?re("layout-sider",C(()=>e.inverted?"a":"b"),L,e):void 0;return Object.assign({scrollableElRef:o,scrollbarInstRef:r,mergedClsPrefix:I,mergedTheme:_,styleMaxWidth:u,mergedCollapsed:i,scrollContainerStyle:v,siderPlacement:c,handleNativeElScroll:N,handleTransitionend:$,handleTriggerClick:x,inlineThemeDisabled:y,cssVars:L,themeClass:M==null?void 0:M.themeClass,onRender:M==null?void 0:M.onRender},F)},render(){var e;const{mergedClsPrefix:t,mergedCollapsed:o,showTrigger:r}=this;return(e=this.onRender)===null||e===void 0||e.call(this),s("aside",{class:[`${t}-layout-sider`,this.themeClass,`${t}-layout-sider--${this.position}-positioned`,`${t}-layout-sider--${this.siderPlacement}-placement`,this.bordered&&`${t}-layout-sider--bordered`,o&&`${t}-layout-sider--collapsed`,(!o||this.showCollapsedContent)&&`${t}-layout-sider--show-content`],onTransitionend:this.handleTransitionend,style:[this.inlineThemeDisabled?void 0:this.cssVars,{maxWidth:this.styleMaxWidth,width:ve(this.width)}]},this.nativeScrollbar?s("div",{class:[`${t}-layout-sider-scroll-container`,this.contentClass],onScroll:this.handleNativeElScroll,style:[this.scrollContainerStyle,{overflow:"auto"},this.contentStyle],ref:"scrollableElRef"},this.$slots):s(je,Object.assign({},this.scrollbarProps,{onScroll:this.onScroll,ref:"scrollbarInstRef",style:this.scrollContainerStyle,contentStyle:this.contentStyle,contentClass:this.contentClass,theme:this.mergedTheme.peers.Scrollbar,themeOverrides:this.mergedTheme.peerOverrides.Scrollbar,builtinThemeOverrides:this.inverted&&this.cssVars.__invertScrollbar==="true"?{colorHover:"rgba(255, 255, 255, .4)",color:"rgba(255, 255, 255, .3)"}:void 0}),this.$slots),r?r==="bar"?s(st,{clsPrefix:t,class:o?this.collapsedTriggerClass:this.triggerClass,style:o?this.collapsedTriggerStyle:this.triggerStyle,onClick:this.handleTriggerClick}):s(ct,{clsPrefix:t,class:o?this.collapsedTriggerClass:this.triggerClass,style:o?this.collapsedTriggerStyle:this.triggerStyle,onClick:this.handleTriggerClick}):null,this.bordered?s("div",{class:`${t}-layout-sider__border`}):null)}}),ie=ne("n-menu"),Ye=ne("n-submenu"),we=ne("n-menu-item-group"),Pe=[g("&::before","background-color: var(--n-item-color-hover);"),m("arrow",`
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
 `,[z("horizontal",`
 max-width: 100%;
 width: 100%;
 display: flex;
 overflow: hidden;
 padding-bottom: 0;
 `,[d("submenu","margin: 0;"),d("menu-item","margin: 0;"),d("menu-item-content",`
 padding: 0 20px;
 border-bottom: 2px solid #0000;
 `,[g("&::before","display: none;"),z("selected","border-bottom: 2px solid var(--n-border-color-horizontal)")]),d("menu-item-content",[z("selected",[m("icon","color: var(--n-item-icon-color-active-horizontal);"),d("menu-item-content-header",`
 color: var(--n-item-text-color-active-horizontal);
 `,[g("a","color: var(--n-item-text-color-active-horizontal);"),m("extra","color: var(--n-item-text-color-active-horizontal);")])]),z("child-active",`
 border-bottom: 2px solid var(--n-border-color-horizontal);
 `,[d("menu-item-content-header",`
 color: var(--n-item-text-color-child-active-horizontal);
 `,[g("a",`
 color: var(--n-item-text-color-child-active-horizontal);
 `),m("extra",`
 color: var(--n-item-text-color-child-active-horizontal);
 `)]),m("icon",`
 color: var(--n-item-icon-color-child-active-horizontal);
 `)]),oe("disabled",[oe("selected, child-active",[g("&:focus-within",Ne)]),z("selected",[X(null,[m("icon","color: var(--n-item-icon-color-active-hover-horizontal);"),d("menu-item-content-header",`
 color: var(--n-item-text-color-active-hover-horizontal);
 `,[g("a","color: var(--n-item-text-color-active-hover-horizontal);"),m("extra","color: var(--n-item-text-color-active-hover-horizontal);")])])]),z("child-active",[X(null,[m("icon","color: var(--n-item-icon-color-child-active-hover-horizontal);"),d("menu-item-content-header",`
 color: var(--n-item-text-color-child-active-hover-horizontal);
 `,[g("a","color: var(--n-item-text-color-child-active-hover-horizontal);"),m("extra","color: var(--n-item-text-color-child-active-hover-horizontal);")])])]),X("border-bottom: 2px solid var(--n-border-color-horizontal);",Ne)]),d("menu-item-content-header",[g("a","color: var(--n-item-text-color-horizontal);")])])]),oe("responsive",[d("menu-item-content-header",`
 overflow: hidden;
 text-overflow: ellipsis;
 `)]),z("collapsed",[d("menu-item-content",[z("selected",[g("&::before",`
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
 `),z("disabled",`
 opacity: .45;
 cursor: not-allowed;
 `),z("collapsed",[m("arrow","transform: rotate(0);")]),z("selected",[g("&::before","background-color: var(--n-item-color-active);"),m("arrow","color: var(--n-arrow-color-active);"),m("icon","color: var(--n-item-icon-color-active);"),d("menu-item-content-header",`
 color: var(--n-item-text-color-active);
 `,[g("a","color: var(--n-item-text-color-active);"),m("extra","color: var(--n-item-text-color-active);")])]),z("child-active",[d("menu-item-content-header",`
 color: var(--n-item-text-color-child-active);
 `,[g("a",`
 color: var(--n-item-text-color-child-active);
 `),m("extra",`
 color: var(--n-item-text-color-child-active);
 `)]),m("arrow",`
 color: var(--n-arrow-color-child-active);
 `),m("icon",`
 color: var(--n-item-icon-color-child-active);
 `)]),oe("disabled",[oe("selected, child-active",[g("&:focus-within",Pe)]),z("selected",[X(null,[m("arrow","color: var(--n-arrow-color-active-hover);"),m("icon","color: var(--n-item-icon-color-active-hover);"),d("menu-item-content-header",`
 color: var(--n-item-text-color-active-hover);
 `,[g("a","color: var(--n-item-text-color-active-hover);"),m("extra","color: var(--n-item-text-color-active-hover);")])])]),z("child-active",[X(null,[m("arrow","color: var(--n-arrow-color-child-active-hover);"),m("icon","color: var(--n-item-icon-color-child-active-hover);"),d("menu-item-content-header",`
 color: var(--n-item-text-color-child-active-hover);
 `,[g("a","color: var(--n-item-text-color-child-active-hover);"),m("extra","color: var(--n-item-text-color-child-active-hover);")])])]),z("selected",[X(null,[g("&::before","background-color: var(--n-item-color-active-hover);")])]),X(null,Pe)]),m("icon",`
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
 `)]);function X(e,t){return[z("hover",e,t),g("&:hover",e,t)]}const Ue=P({name:"MenuOptionContent",props:{collapsed:Boolean,disabled:Boolean,title:[String,Function],icon:Function,extra:[String,Function],showArrow:Boolean,childActive:Boolean,hover:Boolean,paddingLeft:Number,selected:Boolean,maxIconSize:{type:Number,required:!0},activeIconSize:{type:Number,required:!0},iconMarginRight:{type:Number,required:!0},clsPrefix:{type:String,required:!0},onClick:Function,tmNode:{type:Object,required:!0},isEllipsisPlaceholder:Boolean},setup(e){const{props:t}=G(ie);return{menuProps:t,style:C(()=>{const{paddingLeft:o}=e;return{paddingLeft:o&&`${o}px`}}),iconStyle:C(()=>{const{maxIconSize:o,activeIconSize:r,iconMarginRight:l}=e;return{width:`${o}px`,height:`${o}px`,fontSize:`${r}px`,marginRight:`${l}px`}})}},render(){const{clsPrefix:e,tmNode:t,menuProps:{renderIcon:o,renderLabel:r,renderExtra:l,expandIcon:i}}=this,u=o?o(t.rawNode):Q(this.icon);return s("div",{onClick:v=>{var c;(c=this.onClick)===null||c===void 0||c.call(this,v)},role:"none",class:[`${e}-menu-item-content`,{[`${e}-menu-item-content--selected`]:this.selected,[`${e}-menu-item-content--collapsed`]:this.collapsed,[`${e}-menu-item-content--child-active`]:this.childActive,[`${e}-menu-item-content--disabled`]:this.disabled,[`${e}-menu-item-content--hover`]:this.hover}],style:this.style},u&&s("div",{class:`${e}-menu-item-content__icon`,style:this.iconStyle,role:"none"},[u]),s("div",{class:`${e}-menu-item-content-header`,role:"none"},this.isEllipsisPlaceholder?this.title:r?r(t.rawNode):Q(this.title),this.extra||l?s("span",{class:`${e}-menu-item-content-header__extra`}," ",l?l(t.rawNode):Q(this.extra)):null),this.showArrow?s(Le,{ariaHidden:!0,class:`${e}-menu-item-content__arrow`,clsPrefix:e},{default:()=>i?i(t.rawNode):s(Lo,null)}):null)}}),se=8;function ze(e){const t=G(ie),{props:o,mergedCollapsedRef:r}=t,l=G(Ye,null),i=G(we,null),u=C(()=>o.mode==="horizontal"),v=C(()=>u.value?o.dropdownPlacement:"tmNodes"in e?"right-start":"right"),c=C(()=>{var h;return Math.max((h=o.collapsedIconSize)!==null&&h!==void 0?h:o.iconSize,o.iconSize)}),f=C(()=>{var h;return!u.value&&e.root&&r.value&&(h=o.collapsedIconSize)!==null&&h!==void 0?h:o.iconSize}),x=C(()=>{if(u.value)return;const{collapsedWidth:h,indent:N,rootIndent:I}=o,{root:y,isGroup:_}=e,$=I===void 0?N:I;return y?r.value?h/2-c.value/2:$:i&&typeof i.paddingLeftRef.value=="number"?N/2+i.paddingLeftRef.value:l&&typeof l.paddingLeftRef.value=="number"?(_?N/2:N)+l.paddingLeftRef.value:0}),b=C(()=>{const{collapsedWidth:h,indent:N,rootIndent:I}=o,{value:y}=c,{root:_}=e;return u.value||!_||!r.value?se:(I===void 0?N:I)+y+se-(h+y)/2});return{dropdownPlacement:v,activeIconSize:f,maxIconSize:c,paddingLeft:x,iconMarginRight:b,NMenu:t,NSubmenu:l,NMenuOptionGroup:i}}const _e={internalKey:{type:[String,Number],required:!0},root:Boolean,isGroup:Boolean,level:{type:Number,required:!0},title:[String,Function],extra:[String,Function]},mt=P({name:"MenuDivider",setup(){const e=G(ie),{mergedClsPrefixRef:t,isHorizontalRef:o}=e;return()=>o.value?null:s("div",{class:`${t.value}-menu-divider`})}}),We=Object.assign(Object.assign({},_e),{tmNode:{type:Object,required:!0},disabled:Boolean,icon:Function,onClick:Function}),ht=xe(We),ft=P({name:"MenuOption",props:We,setup(e){const t=ze(e),{NSubmenu:o,NMenu:r,NMenuOptionGroup:l}=t,{props:i,mergedClsPrefixRef:u,mergedCollapsedRef:v}=r,c=o?o.mergedDisabledRef:l?l.mergedDisabledRef:{value:!1},f=C(()=>c.value||e.disabled);function x(h){const{onClick:N}=e;N&&N(h)}function b(h){f.value||(r.doSelect(e.internalKey,e.tmNode.rawNode),x(h))}return{mergedClsPrefix:u,dropdownPlacement:t.dropdownPlacement,paddingLeft:t.paddingLeft,iconMarginRight:t.iconMarginRight,maxIconSize:t.maxIconSize,activeIconSize:t.activeIconSize,mergedTheme:r.mergedThemeRef,menuProps:i,dropdownEnabled:he(()=>e.root&&v.value&&i.mode!=="horizontal"&&!f.value),selected:he(()=>r.mergedValueRef.value===e.internalKey),mergedDisabled:f,handleClick:b}},render(){const{mergedClsPrefix:e,mergedTheme:t,tmNode:o,menuProps:{renderLabel:r,nodeProps:l}}=this,i=l==null?void 0:l(o.rawNode);return s("div",Object.assign({},i,{role:"menuitem",class:[`${e}-menu-item`,i==null?void 0:i.class]}),s(Bo,{theme:t.peers.Tooltip,themeOverrides:t.peerOverrides.Tooltip,trigger:"hover",placement:this.dropdownPlacement,disabled:!this.dropdownEnabled||this.title===void 0,internalExtraClass:["menu-tooltip"]},{default:()=>r?r(o.rawNode):Q(this.title),trigger:()=>s(Ue,{tmNode:o,clsPrefix:e,paddingLeft:this.paddingLeft,iconMarginRight:this.iconMarginRight,maxIconSize:this.maxIconSize,activeIconSize:this.activeIconSize,selected:this.selected,title:this.title,extra:this.extra,disabled:this.mergedDisabled,icon:this.icon,onClick:this.handleClick})}))}}),qe=Object.assign(Object.assign({},_e),{tmNode:{type:Object,required:!0},tmNodes:{type:Array,required:!0}}),pt=xe(qe),gt=P({name:"MenuOptionGroup",props:qe,setup(e){const t=ze(e),{NSubmenu:o}=t,r=C(()=>o!=null&&o.mergedDisabledRef.value?!0:e.tmNode.disabled);ee(we,{paddingLeftRef:t.paddingLeft,mergedDisabledRef:r});const{mergedClsPrefixRef:l,props:i}=G(ie);return function(){const{value:u}=l,v=t.paddingLeft.value,{nodeProps:c}=i,f=c==null?void 0:c(e.tmNode.rawNode);return s("div",{class:`${u}-menu-item-group`,role:"group"},s("div",Object.assign({},f,{class:[`${u}-menu-item-group-title`,f==null?void 0:f.class],style:[(f==null?void 0:f.style)||"",v!==void 0?`padding-left: ${v}px;`:""]}),Q(e.title),e.extra?s(Co,null," ",Q(e.extra)):null),s("div",null,e.tmNodes.map(x=>Ie(x,i))))}}});function pe(e){return e.type==="divider"||e.type==="render"}function bt(e){return e.type==="divider"}function Ie(e,t){const{rawNode:o}=e,{show:r}=o;if(r===!1)return null;if(pe(o))return bt(o)?s(mt,Object.assign({key:e.key},o.props)):null;const{labelField:l}=t,{key:i,level:u,isGroup:v}=e,c=Object.assign(Object.assign({},o),{title:o.title||o[l],extra:o.titleExtra||o.extra,key:i,internalKey:i,level:u,root:u===0,isGroup:v});return e.children?e.isGroup?s(gt,ue(c,pt,{tmNode:e,tmNodes:e.children,key:i})):s(ge,ue(c,xt,{key:i,rawNodes:o[t.childrenField],tmNodes:e.children,tmNode:e})):s(ft,ue(c,ht,{key:i,tmNode:e}))}const Ge=Object.assign(Object.assign({},_e),{rawNodes:{type:Array,default:()=>[]},tmNodes:{type:Array,default:()=>[]},tmNode:{type:Object,required:!0},disabled:Boolean,icon:Function,onClick:Function,domId:String,virtualChildActive:{type:Boolean,default:void 0},isEllipsisPlaceholder:Boolean}),xt=xe(Ge),ge=P({name:"Submenu",props:Ge,setup(e){const t=ze(e),{NMenu:o,NSubmenu:r}=t,{props:l,mergedCollapsedRef:i,mergedThemeRef:u}=o,v=C(()=>{const{disabled:h}=e;return r!=null&&r.mergedDisabledRef.value||l.disabled?!0:h}),c=B(!1);ee(Ye,{paddingLeftRef:t.paddingLeft,mergedDisabledRef:v}),ee(we,null);function f(){const{onClick:h}=e;h&&h()}function x(){v.value||(i.value||o.toggleExpand(e.internalKey),f())}function b(h){c.value=h}return{menuProps:l,mergedTheme:u,doSelect:o.doSelect,inverted:o.invertedRef,isHorizontal:o.isHorizontalRef,mergedClsPrefix:o.mergedClsPrefixRef,maxIconSize:t.maxIconSize,activeIconSize:t.activeIconSize,iconMarginRight:t.iconMarginRight,dropdownPlacement:t.dropdownPlacement,dropdownShow:c,paddingLeft:t.paddingLeft,mergedDisabled:v,mergedValue:o.mergedValueRef,childActive:he(()=>{var h;return(h=e.virtualChildActive)!==null&&h!==void 0?h:o.activePathRef.value.includes(e.internalKey)}),collapsed:C(()=>l.mode==="horizontal"?!1:i.value?!0:!o.mergedExpandedKeysRef.value.includes(e.internalKey)),dropdownEnabled:C(()=>!v.value&&(l.mode==="horizontal"||i.value)),handlePopoverShowChange:b,handleClick:x}},render(){var e;const{mergedClsPrefix:t,menuProps:{renderIcon:o,renderLabel:r}}=this,l=()=>{const{isHorizontal:u,paddingLeft:v,collapsed:c,mergedDisabled:f,maxIconSize:x,activeIconSize:b,title:h,childActive:N,icon:I,handleClick:y,menuProps:{nodeProps:_},dropdownShow:$,iconMarginRight:F,tmNode:L,mergedClsPrefix:M,isEllipsisPlaceholder:H,extra:R}=this,A=_==null?void 0:_(L.rawNode);return s("div",Object.assign({},A,{class:[`${M}-menu-item`,A==null?void 0:A.class],role:"menuitem"}),s(Ue,{tmNode:L,paddingLeft:v,collapsed:c,disabled:f,iconMarginRight:F,maxIconSize:x,activeIconSize:b,title:h,extra:R,showArrow:!u,childActive:N,clsPrefix:M,icon:I,hover:$,onClick:y,isEllipsisPlaceholder:H}))},i=()=>s(Oe,null,{default:()=>{const{tmNodes:u,collapsed:v}=this;return v?null:s("div",{class:`${t}-submenu-children`,role:"menu"},u.map(c=>Ie(c,this.menuProps)))}});return this.root?s(Oo,Object.assign({size:"large",trigger:"hover"},(e=this.menuProps)===null||e===void 0?void 0:e.dropdownProps,{themeOverrides:this.mergedTheme.peerOverrides.Dropdown,theme:this.mergedTheme.peers.Dropdown,builtinThemeOverrides:{fontSizeLarge:"14px",optionIconSizeLarge:"18px"},value:this.mergedValue,disabled:!this.dropdownEnabled,placement:this.dropdownPlacement,keyField:this.menuProps.keyField,labelField:this.menuProps.labelField,childrenField:this.menuProps.childrenField,onUpdateShow:this.handlePopoverShowChange,options:this.rawNodes,onSelect:this.doSelect,inverted:this.inverted,renderIcon:o,renderLabel:r}),{default:()=>s("div",{class:`${t}-submenu`,role:"menu","aria-expanded":!this.collapsed,id:this.domId},l(),this.isHorizontal?null:i())}):s("div",{class:`${t}-submenu`,role:"menu","aria-expanded":!this.collapsed,id:this.domId},l(),i())}}),Ct=Object.assign(Object.assign({},Y.props),{options:{type:Array,default:()=>[]},collapsed:{type:Boolean,default:void 0},collapsedWidth:{type:Number,default:48},iconSize:{type:Number,default:20},collapsedIconSize:{type:Number,default:24},rootIndent:Number,indent:{type:Number,default:32},labelField:{type:String,default:"label"},keyField:{type:String,default:"key"},childrenField:{type:String,default:"children"},disabledField:{type:String,default:"disabled"},defaultExpandAll:Boolean,defaultExpandedKeys:Array,expandedKeys:Array,value:[String,Number],defaultValue:{type:[String,Number],default:null},mode:{type:String,default:"vertical"},watchProps:{type:Array,default:void 0},disabled:Boolean,show:{type:Boolean,default:!0},inverted:Boolean,"onUpdate:expandedKeys":[Function,Array],onUpdateExpandedKeys:[Function,Array],onUpdateValue:[Function,Array],"onUpdate:value":[Function,Array],expandIcon:Function,renderIcon:Function,renderLabel:Function,renderExtra:Function,dropdownProps:Object,accordion:Boolean,nodeProps:Function,dropdownPlacement:{type:String,default:"bottom"},responsive:Boolean,items:Array,onOpenNamesChange:[Function,Array],onSelect:[Function,Array],onExpandedNamesChange:[Function,Array],expandedNames:Array,defaultExpandedNames:Array}),yt=P({name:"Menu",inheritAttrs:!1,props:Ct,setup(e){const{mergedClsPrefixRef:t,inlineThemeDisabled:o}=te(e),r=Y("Menu","-menu",vt,Qo,e,t),l=G(Ve,null),i=C(()=>{var p;const{collapsed:k}=e;if(k!==void 0)return k;if(l){const{collapseModeRef:n,collapsedRef:w}=l;if(n.value==="width")return(p=w.value)!==null&&p!==void 0?p:!1}return!1}),u=C(()=>{const{keyField:p,childrenField:k,disabledField:n}=e;return me(e.items||e.options,{getIgnored(w){return pe(w)},getChildren(w){return w[k]},getDisabled(w){return w[n]},getKey(w){var T;return(T=w[p])!==null&&T!==void 0?T:w.name}})}),v=C(()=>new Set(u.value.treeNodes.map(p=>p.key))),{watchProps:c}=e,f=B(null);c!=null&&c.includes("defaultValue")?$e(()=>{f.value=e.defaultValue}):f.value=e.defaultValue;const x=Z(e,"value"),b=fe(x,f),h=B([]),N=()=>{h.value=e.defaultExpandAll?u.value.getNonLeafKeys():e.defaultExpandedNames||e.defaultExpandedKeys||u.value.getPath(b.value,{includeSelf:!1}).keyPath};c!=null&&c.includes("defaultExpandedKeys")?$e(N):N();const I=Mo(e,["expandedNames","expandedKeys"]),y=fe(I,h),_=C(()=>u.value.treeNodes),$=C(()=>u.value.getPath(b.value).keyPath);ee(ie,{props:e,mergedCollapsedRef:i,mergedThemeRef:r,mergedValueRef:b,mergedExpandedKeysRef:y,activePathRef:$,mergedClsPrefixRef:t,isHorizontalRef:C(()=>e.mode==="horizontal"),invertedRef:Z(e,"inverted"),doSelect:F,toggleExpand:M});function F(p,k){const{"onUpdate:value":n,onUpdateValue:w,onSelect:T}=e;w&&K(w,p,k),n&&K(n,p,k),T&&K(T,p,k),f.value=p}function L(p){const{"onUpdate:expandedKeys":k,onUpdateExpandedKeys:n,onExpandedNamesChange:w,onOpenNamesChange:T}=e;k&&K(k,p),n&&K(n,p),w&&K(w,p),T&&K(T,p),h.value=p}function M(p){const k=Array.from(y.value),n=k.findIndex(w=>w===p);if(~n)k.splice(n,1);else{if(e.accordion&&v.value.has(p)){const w=k.findIndex(T=>v.value.has(T));w>-1&&k.splice(w,1)}k.push(p)}L(k)}const H=p=>{const k=u.value.getPath(p??b.value,{includeSelf:!1}).keyPath;if(!k.length)return;const n=Array.from(y.value),w=new Set([...n,...k]);e.accordion&&v.value.forEach(T=>{w.has(T)&&!k.includes(T)&&w.delete(T)}),L(Array.from(w))},R=C(()=>{const{inverted:p}=e,{common:{cubicBezierEaseInOut:k},self:n}=r.value,{borderRadius:w,borderColorHorizontal:T,fontSize:ro,itemHeight:no,dividerColor:io}=n,a={"--n-divider-color":io,"--n-bezier":k,"--n-font-size":ro,"--n-border-color-horizontal":T,"--n-border-radius":w,"--n-item-height":no};return p?(a["--n-group-text-color"]=n.groupTextColorInverted,a["--n-color"]=n.colorInverted,a["--n-item-text-color"]=n.itemTextColorInverted,a["--n-item-text-color-hover"]=n.itemTextColorHoverInverted,a["--n-item-text-color-active"]=n.itemTextColorActiveInverted,a["--n-item-text-color-child-active"]=n.itemTextColorChildActiveInverted,a["--n-item-text-color-child-active-hover"]=n.itemTextColorChildActiveInverted,a["--n-item-text-color-active-hover"]=n.itemTextColorActiveHoverInverted,a["--n-item-icon-color"]=n.itemIconColorInverted,a["--n-item-icon-color-hover"]=n.itemIconColorHoverInverted,a["--n-item-icon-color-active"]=n.itemIconColorActiveInverted,a["--n-item-icon-color-active-hover"]=n.itemIconColorActiveHoverInverted,a["--n-item-icon-color-child-active"]=n.itemIconColorChildActiveInverted,a["--n-item-icon-color-child-active-hover"]=n.itemIconColorChildActiveHoverInverted,a["--n-item-icon-color-collapsed"]=n.itemIconColorCollapsedInverted,a["--n-item-text-color-horizontal"]=n.itemTextColorHorizontalInverted,a["--n-item-text-color-hover-horizontal"]=n.itemTextColorHoverHorizontalInverted,a["--n-item-text-color-active-horizontal"]=n.itemTextColorActiveHorizontalInverted,a["--n-item-text-color-child-active-horizontal"]=n.itemTextColorChildActiveHorizontalInverted,a["--n-item-text-color-child-active-hover-horizontal"]=n.itemTextColorChildActiveHoverHorizontalInverted,a["--n-item-text-color-active-hover-horizontal"]=n.itemTextColorActiveHoverHorizontalInverted,a["--n-item-icon-color-horizontal"]=n.itemIconColorHorizontalInverted,a["--n-item-icon-color-hover-horizontal"]=n.itemIconColorHoverHorizontalInverted,a["--n-item-icon-color-active-horizontal"]=n.itemIconColorActiveHorizontalInverted,a["--n-item-icon-color-active-hover-horizontal"]=n.itemIconColorActiveHoverHorizontalInverted,a["--n-item-icon-color-child-active-horizontal"]=n.itemIconColorChildActiveHorizontalInverted,a["--n-item-icon-color-child-active-hover-horizontal"]=n.itemIconColorChildActiveHoverHorizontalInverted,a["--n-arrow-color"]=n.arrowColorInverted,a["--n-arrow-color-hover"]=n.arrowColorHoverInverted,a["--n-arrow-color-active"]=n.arrowColorActiveInverted,a["--n-arrow-color-active-hover"]=n.arrowColorActiveHoverInverted,a["--n-arrow-color-child-active"]=n.arrowColorChildActiveInverted,a["--n-arrow-color-child-active-hover"]=n.arrowColorChildActiveHoverInverted,a["--n-item-color-hover"]=n.itemColorHoverInverted,a["--n-item-color-active"]=n.itemColorActiveInverted,a["--n-item-color-active-hover"]=n.itemColorActiveHoverInverted,a["--n-item-color-active-collapsed"]=n.itemColorActiveCollapsedInverted):(a["--n-group-text-color"]=n.groupTextColor,a["--n-color"]=n.color,a["--n-item-text-color"]=n.itemTextColor,a["--n-item-text-color-hover"]=n.itemTextColorHover,a["--n-item-text-color-active"]=n.itemTextColorActive,a["--n-item-text-color-child-active"]=n.itemTextColorChildActive,a["--n-item-text-color-child-active-hover"]=n.itemTextColorChildActiveHover,a["--n-item-text-color-active-hover"]=n.itemTextColorActiveHover,a["--n-item-icon-color"]=n.itemIconColor,a["--n-item-icon-color-hover"]=n.itemIconColorHover,a["--n-item-icon-color-active"]=n.itemIconColorActive,a["--n-item-icon-color-active-hover"]=n.itemIconColorActiveHover,a["--n-item-icon-color-child-active"]=n.itemIconColorChildActive,a["--n-item-icon-color-child-active-hover"]=n.itemIconColorChildActiveHover,a["--n-item-icon-color-collapsed"]=n.itemIconColorCollapsed,a["--n-item-text-color-horizontal"]=n.itemTextColorHorizontal,a["--n-item-text-color-hover-horizontal"]=n.itemTextColorHoverHorizontal,a["--n-item-text-color-active-horizontal"]=n.itemTextColorActiveHorizontal,a["--n-item-text-color-child-active-horizontal"]=n.itemTextColorChildActiveHorizontal,a["--n-item-text-color-child-active-hover-horizontal"]=n.itemTextColorChildActiveHoverHorizontal,a["--n-item-text-color-active-hover-horizontal"]=n.itemTextColorActiveHoverHorizontal,a["--n-item-icon-color-horizontal"]=n.itemIconColorHorizontal,a["--n-item-icon-color-hover-horizontal"]=n.itemIconColorHoverHorizontal,a["--n-item-icon-color-active-horizontal"]=n.itemIconColorActiveHorizontal,a["--n-item-icon-color-active-hover-horizontal"]=n.itemIconColorActiveHoverHorizontal,a["--n-item-icon-color-child-active-horizontal"]=n.itemIconColorChildActiveHorizontal,a["--n-item-icon-color-child-active-hover-horizontal"]=n.itemIconColorChildActiveHoverHorizontal,a["--n-arrow-color"]=n.arrowColor,a["--n-arrow-color-hover"]=n.arrowColorHover,a["--n-arrow-color-active"]=n.arrowColorActive,a["--n-arrow-color-active-hover"]=n.arrowColorActiveHover,a["--n-arrow-color-child-active"]=n.arrowColorChildActive,a["--n-arrow-color-child-active-hover"]=n.arrowColorChildActiveHover,a["--n-item-color-hover"]=n.itemColorHover,a["--n-item-color-active"]=n.itemColorActive,a["--n-item-color-active-hover"]=n.itemColorActiveHover,a["--n-item-color-active-collapsed"]=n.itemColorActiveCollapsed),a}),A=o?re("menu",C(()=>e.inverted?"a":"b"),R,e):void 0,W=wo(),U=B(null),ce=B(null);let j=!0;const ke=()=>{var p;j?j=!1:(p=U.value)===null||p===void 0||p.sync({showAllItemsBeforeCalculate:!0})};function Xe(){return document.getElementById(W)}const le=B(-1);function Ze(p){le.value=e.options.length-p}function Je(p){p||(le.value=-1)}const Qe=C(()=>{const p=le.value;return{children:p===-1?[]:e.options.slice(p)}}),eo=C(()=>{const{childrenField:p,disabledField:k,keyField:n}=e;return me([Qe.value],{getIgnored(w){return pe(w)},getChildren(w){return w[p]},getDisabled(w){return w[k]},getKey(w){var T;return(T=w[n])!==null&&T!==void 0?T:w.name}})}),oo=C(()=>me([{}]).treeNodes[0]);function to(){var p;if(le.value===-1)return s(ge,{root:!0,level:0,key:"__ellpisisGroupPlaceholder__",internalKey:"__ellpisisGroupPlaceholder__",title:"···",tmNode:oo.value,domId:W,isEllipsisPlaceholder:!0});const k=eo.value.treeNodes[0],n=$.value,w=!!(!((p=k.children)===null||p===void 0)&&p.some(T=>n.includes(T.key)));return s(ge,{level:0,root:!0,key:"__ellpisisGroup__",internalKey:"__ellpisisGroup__",title:"···",virtualChildActive:w,tmNode:k,domId:W,rawNodes:k.rawNode.children||[],tmNodes:k.children||[],isEllipsisPlaceholder:!0})}return{mergedClsPrefix:t,controlledExpandedKeys:I,uncontrolledExpanededKeys:h,mergedExpandedKeys:y,uncontrolledValue:f,mergedValue:b,activePath:$,tmNodes:_,mergedTheme:r,mergedCollapsed:i,cssVars:o?void 0:R,themeClass:A==null?void 0:A.themeClass,overflowRef:U,counterRef:ce,updateCounter:()=>{},onResize:ke,onUpdateOverflow:Je,onUpdateCount:Ze,renderCounter:to,getCounter:Xe,onRender:A==null?void 0:A.onRender,showOption:H,deriveResponsiveState:ke}},render(){const{mergedClsPrefix:e,mode:t,themeClass:o,onRender:r}=this;r==null||r();const l=()=>this.tmNodes.map(c=>Ie(c,this.$props)),u=t==="horizontal"&&this.responsive,v=()=>s("div",zo(this.$attrs,{role:t==="horizontal"?"menubar":"menu",class:[`${e}-menu`,o,`${e}-menu--${t}`,u&&`${e}-menu--responsive`,this.mergedCollapsed&&`${e}-menu--collapsed`],style:this.cssVars}),u?s(jo,{ref:"overflowRef",onUpdateOverflow:this.onUpdateOverflow,getCounter:this.getCounter,onUpdateCount:this.onUpdateCount,updateCounter:this.updateCounter,style:{width:"100%",display:"flex",overflow:"hidden"}},{default:l,counter:this.renderCounter}):l());return u?s(yo,{onResize:this.onResize},{default:v}):v()}}),wt={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},zt=S("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32",d:"M256 112v288"},null,-1),_t=S("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32",d:"M400 256H112"},null,-1),It=[zt,_t],kt=P({name:"AddOutline",render:function(t,o){return V(),D("svg",wt,It)}}),St={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},Rt=S("path",{d:"M32 32v432a16 16 0 0 0 16 16h432",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),At=S("rect",{x:"96",y:"224",width:"80",height:"192",rx:"20",ry:"20",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),$t=S("rect",{x:"240",y:"176",width:"80",height:"240",rx:"20",ry:"20",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),Ht=S("rect",{x:"383.64",y:"112",width:"80",height:"304",rx:"20",ry:"20",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),Pt=[Rt,At,$t,Ht],Te=P({name:"BarChartOutline",render:function(t,o){return V(),D("svg",St,Pt)}}),Nt={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},Tt=S("path",{d:"M440 432H72a40 40 0 0 1-40-40V120a40 40 0 0 1 40-40h75.89a40 40 0 0 1 22.19 6.72l27.84 18.56a40 40 0 0 0 22.19 6.72H440a40 40 0 0 1 40 40v240a40 40 0 0 1-40 40z",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),Bt=S("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32",d:"M32 192h448"},null,-1),Ot=[Tt,Bt],Mt=P({name:"FolderOutline",render:function(t,o){return V(),D("svg",Nt,Ot)}}),Et={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},jt=S("path",{d:"M80 212v236a16 16 0 0 0 16 16h96V328a24 24 0 0 1 24-24h80a24 24 0 0 1 24 24v136h96a16 16 0 0 0 16-16V212",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),Ft=S("path",{d:"M480 256L266.89 52c-5-5.28-16.69-5.34-21.78 0L32 256",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),Lt=S("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32",d:"M400 179V64h-48v69"},null,-1),Vt=[jt,Ft,Lt],Kt=P({name:"HomeOutline",render:function(t,o){return V(),D("svg",Et,Vt)}}),Dt={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},Yt=S("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-miterlimit":"10","stroke-width":"32",d:"M80 160h352"},null,-1),Ut=S("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-miterlimit":"10","stroke-width":"32",d:"M80 256h352"},null,-1),Wt=S("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-miterlimit":"10","stroke-width":"32",d:"M80 352h352"},null,-1),qt=[Yt,Ut,Wt],Gt=P({name:"MenuOutline",render:function(t,o){return V(),D("svg",Dt,qt)}}),Xt={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},Zt=S("path",{d:"M427.68 351.43C402 320 383.87 304 383.87 217.35C383.87 138 343.35 109.73 310 96c-4.43-1.82-8.6-6-9.95-10.55C294.2 65.54 277.8 48 256 48s-38.21 17.55-44 37.47c-1.35 4.6-5.52 8.71-9.95 10.53c-33.39 13.75-73.87 41.92-73.87 121.35C128.13 304 110 320 84.32 351.43C73.68 364.45 83 384 101.61 384h308.88c18.51 0 27.77-19.61 17.19-32.57z",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),Jt=S("path",{d:"M320 384v16a64 64 0 0 1-128 0v-16",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),Qt=[Zt,Jt],er=P({name:"NotificationsOutline",render:function(t,o){return V(),D("svg",Xt,Qt)}}),or={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},tr=S("path",{fill:"none",stroke:"currentColor","stroke-linejoin":"round","stroke-width":"32",d:"M160 336V48l32 16l32-16l31.94 16l32.37-16L320 64l31.79-16l31.93 16L416 48l32.01 16L480 48v224"},null,-1),rr=S("path",{d:"M480 272v112a80 80 0 0 1-80 80h0a80 80 0 0 1-80-80v-48H48a15.86 15.86 0 0 0-16 16c0 64 6.74 112 80 112h288",fill:"none",stroke:"currentColor","stroke-linejoin":"round","stroke-width":"32"},null,-1),nr=S("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32",d:"M224 144h192"},null,-1),ir=S("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32",d:"M288 224h128"},null,-1),lr=[tr,rr,nr,ir],ar=P({name:"ReceiptOutline",render:function(t,o){return V(),D("svg",or,lr)}}),sr={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},cr=S("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32",d:"M320 120l48 48l-48 48"},null,-1),dr=S("path",{d:"M352 168H144a80.24 80.24 0 0 0-80 80v16",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),ur=S("path",{fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32",d:"M192 392l-48-48l48-48"},null,-1),vr=S("path",{d:"M160 344h208a80.24 80.24 0 0 0 80-80v-16",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),mr=[cr,dr,ur,vr],hr=P({name:"RepeatOutline",render:function(t,o){return V(),D("svg",sr,mr)}}),fr={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},pr=S("path",{d:"M262.29 192.31a64 64 0 1 0 57.4 57.4a64.13 64.13 0 0 0-57.4-57.4zM416.39 256a154.34 154.34 0 0 1-1.53 20.79l45.21 35.46a10.81 10.81 0 0 1 2.45 13.75l-42.77 74a10.81 10.81 0 0 1-13.14 4.59l-44.9-18.08a16.11 16.11 0 0 0-15.17 1.75A164.48 164.48 0 0 1 325 400.8a15.94 15.94 0 0 0-8.82 12.14l-6.73 47.89a11.08 11.08 0 0 1-10.68 9.17h-85.54a11.11 11.11 0 0 1-10.69-8.87l-6.72-47.82a16.07 16.07 0 0 0-9-12.22a155.3 155.3 0 0 1-21.46-12.57a16 16 0 0 0-15.11-1.71l-44.89 18.07a10.81 10.81 0 0 1-13.14-4.58l-42.77-74a10.8 10.8 0 0 1 2.45-13.75l38.21-30a16.05 16.05 0 0 0 6-14.08c-.36-4.17-.58-8.33-.58-12.5s.21-8.27.58-12.35a16 16 0 0 0-6.07-13.94l-38.19-30A10.81 10.81 0 0 1 49.48 186l42.77-74a10.81 10.81 0 0 1 13.14-4.59l44.9 18.08a16.11 16.11 0 0 0 15.17-1.75A164.48 164.48 0 0 1 187 111.2a15.94 15.94 0 0 0 8.82-12.14l6.73-47.89A11.08 11.08 0 0 1 213.23 42h85.54a11.11 11.11 0 0 1 10.69 8.87l6.72 47.82a16.07 16.07 0 0 0 9 12.22a155.3 155.3 0 0 1 21.46 12.57a16 16 0 0 0 15.11 1.71l44.89-18.07a10.81 10.81 0 0 1 13.14 4.58l42.77 74a10.8 10.8 0 0 1-2.45 13.75l-38.21 30a16.05 16.05 0 0 0-6.05 14.08c.33 4.14.55 8.3.55 12.47z",fill:"none",stroke:"currentColor","stroke-linecap":"round","stroke-linejoin":"round","stroke-width":"32"},null,-1),gr=[pr],br=P({name:"SettingsOutline",render:function(t,o){return V(),D("svg",fr,gr)}}),xr={xmlns:"http://www.w3.org/2000/svg","xmlns:xlink":"http://www.w3.org/1999/xlink",viewBox:"0 0 512 512"},Cr=S("rect",{x:"48",y:"144",width:"416",height:"288",rx:"48",ry:"48",fill:"none",stroke:"currentColor","stroke-linejoin":"round","stroke-width":"32"},null,-1),yr=S("path",{d:"M411.36 144v-30A50 50 0 0 0 352 64.9L88.64 109.85A50 50 0 0 0 48 159v49",fill:"none",stroke:"currentColor","stroke-linejoin":"round","stroke-width":"32"},null,-1),wr=S("path",{d:"M368 320a32 32 0 1 1 32-32a32 32 0 0 1-32 32z",fill:"currentColor"},null,-1),zr=[Cr,yr,wr],_r=P({name:"WalletOutline",render:function(t,o){return V(),D("svg",xr,zr)}}),Ir={class:"logo"},kr={key:0},Sr={style:{"font-size":"16px"}},Rr={__name:"Layout",setup(e){const t=Io(),o=ko(),r=B(!1);Me(()=>{window.innerWidth<768&&(r.value=!0)});const l=x=>()=>s(ae,null,{default:()=>s(x)}),i=[{label:"首页",key:"Dashboard",icon:l(Kt)},{label:"交易",key:"Transactions",icon:l(ar)},{label:"账户",key:"Accounts",icon:l(_r)},{label:"分类",key:"Categories",icon:l(Mt)},{label:"预算",key:"Budget",icon:l(Te)},{label:"周期",key:"Recurring",icon:l(hr)},{label:"报表",key:"Reports",icon:l(Te)},{label:"设置",key:"Settings",icon:l(br)}],u=C(()=>o.name||"Dashboard"),v=B(0),c=C(()=>({Dashboard:"首页",Transactions:"交易记录",AddTransaction:"新增交易",EditTransaction:"编辑交易",Accounts:"账户管理",Categories:"分类管理",Budget:"预算管理",Recurring:"周期交易",Reports:"统计报表",Settings:"系统设置"})[o.name]||"BudgetPilot"),f=x=>{t.push({name:x}),window.innerWidth<768&&(r.value=!0)};return(x,b)=>{const h=yt,N=ut,I=Ao,y=Fo,_=Go,$=lt,F=So("router-view"),L=rt,M=tt;return V(),_o(M,{"has-sider":"",style:{height:"100vh"},"native-scrollbar":!1},{default:E(()=>[O(N,{bordered:"",collapsed:r.value,width:220,"collapsed-width":0,"show-trigger":"bar","collapse-mode":"transform",onCollapse:b[0]||(b[0]=H=>r.value=!0),onExpand:b[1]||(b[1]=H=>r.value=!1),"native-scrollbar":!1,class:"sider-wrapper"},{default:E(()=>[S("div",Ir,[r.value?Ro("",!0):(V(),D("h2",kr,"BudgetPilot"))]),O(h,{collapsed:r.value,"collapsed-width":0,"collapsed-icon-size":22,options:i,value:u.value,"onUpdate:value":f},null,8,["collapsed","value"])]),_:1},8,["collapsed"]),O(M,{style:{height:"100vh"},"native-scrollbar":!1},{default:E(()=>[O($,{bordered:"",style:{padding:"12px 16px",background:"#fff",display:"flex","justify-content":"space-between","align-items":"center"}},{default:E(()=>[O(y,{size:12,align:"center"},{default:E(()=>[O(I,{quaternary:"",circle:"",size:"small",onClick:b[2]||(b[2]=H=>r.value=!r.value),class:"menu-toggle"},{icon:E(()=>[O(q(ae),null,{default:E(()=>[O(q(Gt))]),_:1})]),_:1}),S("h3",Sr,$o(c.value),1)]),_:1}),O(y,{size:8},{default:E(()=>[O(_,{value:v.value,show:v.value>0},{default:E(()=>[O(I,{quaternary:"",circle:"",size:"small",onClick:b[3]||(b[3]=H=>q(t).push("/alerts"))},{icon:E(()=>[O(q(ae),null,{default:E(()=>[O(q(er))]),_:1})]),_:1})]),_:1},8,["value","show"]),O(I,{type:"primary",size:"small",onClick:b[4]||(b[4]=H=>q(t).push("/transactions/add")),class:"add-btn"},{icon:E(()=>[O(q(ae),null,{default:E(()=>[O(q(kt))]),_:1})]),default:E(()=>[b[5]||(b[5]=S("span",{class:"add-text"},"记一笔",-1))]),_:1})]),_:1})]),_:1}),O(L,{"content-style":"padding: 16px;"},{default:E(()=>[O(F)]),_:1})]),_:1})]),_:1})}}},Mr=Ho(Rr,[["__scopeId","data-v-5caef19f"]]);export{Mr as default};
