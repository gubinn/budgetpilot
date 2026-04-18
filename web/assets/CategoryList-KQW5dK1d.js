import{c as we}from"./index-DOV74WaB.js";import{_ as ft}from"./Tree-YcmrG7N7.js";import{g as m,o as R,n as $,d as N,h as i,u as Ke,ac as mt,c as gt,ad as vt,a as bt,ae,af as K,ag as $e,ah as X,ai as J,aj as Q,ak as pe,al as le,am as Xe,an as De,ao as E,ap as Me,aq as Be,m as P,r as C,ar as he,as as fe,f as xt,q as Ze,D as We,at as be,au as Te,av as Fe,aw as He,ax as yt,ay as kt,i as Ne,az as wt,aA as _t,aB as St,aC as Ct,aD as Ut,j as Ye,a0 as $t,l as Rt,aE as Le,aF as At,t as _e,U as G,aG as zt,v as Ve,aa as Oe,p as It,K as Vt,L as Pt,M as S,_ as Dt,H as Mt,R as k,ab as Bt,W as Pe,$ as de}from"./index-CrOOCnO8.js";import{i as Tt,_ as Je}from"./Input-B2EqWgxj.js";import{V as Ft,a as Ht,u as qe,B as qt}from"./Popover-C8nxGhd6.js";import{b as Et,u as Nt,a as Lt}from"./request-B2DXN-UX.js";import{u as je}from"./get-QTxuVnWK.js";import{u as Ot}from"./composables-xpoC4jHI.js";import{_ as jt,a as Gt}from"./FormItem-CcKJRkBl.js";import{_ as Kt}from"./Select-COsPwSyo.js";import{_ as Xt}from"./Space-Cfoo0Drt.js";import{_ as Zt}from"./RadioGroup-B0933P_C.js";import{_ as Wt}from"./RadioButton-BqOs8wVq.js";import"./Checkbox-DVapcyQG.js";import"./create-CHVB1Dqt.js";import"./Suffix-C3ldkkg-.js";const Yt=m("input-group",`
 display: inline-flex;
 width: 100%;
 flex-wrap: nowrap;
 vertical-align: bottom;
`,[R(">",[m("input",[R("&:not(:last-child)",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `),R("&:not(:first-child)",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 margin-left: -1px!important;
 `)]),m("button",[R("&:not(:last-child)",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `,[$("state-border, border",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `)]),R("&:not(:first-child)",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `,[$("state-border, border",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `)])]),R("*",[R("&:not(:last-child)",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `,[R(">",[m("input",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `),m("base-selection",[m("base-selection-label",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `),m("base-selection-tags",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `),$("box-shadow, border, state-border",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `)])])]),R("&:not(:first-child)",`
 margin-left: -1px!important;
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `,[R(">",[m("input",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `),m("base-selection",[m("base-selection-label",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `),m("base-selection-tags",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `),$("box-shadow, border, state-border",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `)])])])])])]),Jt={},Qt=N({name:"InputGroup",props:Jt,setup(e){const{mergedClsPrefixRef:t}=Ke(e);return mt("-input-group",Yt,t),{mergedClsPrefix:t}},render(){const{mergedClsPrefix:e}=this;return i("div",{class:`${e}-input-group`},this.$slots)}});function er(e){const{fontSize:t,boxShadow2:a,popoverColor:o,textColor2:l,borderRadius:n,borderColor:u,heightSmall:d,heightMedium:c,heightLarge:A,fontSizeSmall:H,fontSizeMedium:D,fontSizeLarge:M,dividerColor:T}=e;return{panelFontSize:t,boxShadow:a,color:o,textColor:l,borderRadius:n,border:`1px solid ${u}`,heightSmall:d,heightMedium:c,heightLarge:A,fontSizeSmall:H,fontSizeMedium:D,fontSizeLarge:M,dividerColor:T}}const tr=gt({name:"ColorPicker",common:bt,peers:{Input:Tt,Button:vt},self:er});function rr(e,t){switch(e[0]){case"hex":return t?"#000000FF":"#000000";case"rgb":return t?"rgba(0, 0, 0, 1)":"rgb(0, 0, 0)";case"hsl":return t?"hsla(0, 0%, 0%, 1)":"hsl(0, 0%, 0%)";case"hsv":return t?"hsva(0, 0%, 0%, 1)":"hsv(0, 0%, 0%)"}return"#000000"}function xe(e){return e===null?null:/^ *#/.test(e)?"hex":e.includes("rgb")?"rgb":e.includes("hsl")?"hsl":e.includes("hsv")?"hsv":null}function or(e,t=[255,255,255],a="AA"){const[o,l,n,u]=E(K(e));if(u===1){const T=Se([o,l,n]),L=Se(t);return(Math.max(T,L)+.05)/(Math.min(T,L)+.05)>=(a==="AA"?4.5:7)}const d=Math.round(o*u+t[0]*(1-u)),c=Math.round(l*u+t[1]*(1-u)),A=Math.round(n*u+t[2]*(1-u)),H=Se([d,c,A]),D=Se(t);return(Math.max(H,D)+.05)/(Math.min(H,D)+.05)>=(a==="AA"?4.5:7)}function Se(e){const[t,a,o]=e.map(l=>(l/=255,l<=.03928?l/12.92:Math.pow((l+.055)/1.055,2.4)));return .2126*t+.7152*a+.0722*o}function nr(e){return e=Math.round(e),e>=360?359:e<0?0:e}function ar(e){return e=Math.round(e*100)/100,e>1?1:e<0?0:e}const lr={rgb:{hex(e){return Q(E(e))},hsl(e){const[t,a,o,l]=E(e);return K([...Be(t,a,o),l])},hsv(e){const[t,a,o,l]=E(e);return le([...Me(t,a,o),l])}},hex:{rgb(e){return X(E(e))},hsl(e){const[t,a,o,l]=E(e);return K([...Be(t,a,o),l])},hsv(e){const[t,a,o,l]=E(e);return le([...Me(t,a,o),l])}},hsl:{hex(e){const[t,a,o,l]=pe(e);return Q([...De(t,a,o),l])},rgb(e){const[t,a,o,l]=pe(e);return X([...De(t,a,o),l])},hsv(e){const[t,a,o,l]=pe(e);return le([...Xe(t,a,o),l])}},hsv:{hex(e){const[t,a,o,l]=ae(e);return Q([...J(t,a,o),l])},rgb(e){const[t,a,o,l]=ae(e);return X([...J(t,a,o),l])},hsl(e){const[t,a,o,l]=ae(e);return K([...$e(t,a,o),l])}}};function Qe(e,t,a){return a=a||xe(e),a?a===t?e:lr[a][t](e):null}const ve="12px",ir=12,oe="6px",sr=N({name:"AlphaSlider",props:{clsPrefix:{type:String,required:!0},rgba:{type:Array,default:null},alpha:{type:Number,default:0},onUpdateAlpha:{type:Function,required:!0},onComplete:Function},setup(e){const t=C(null);function a(n){!t.value||!e.rgba||(he("mousemove",document,o),he("mouseup",document,l),o(n))}function o(n){const{value:u}=t;if(!u)return;const{width:d,left:c}=u.getBoundingClientRect(),A=(n.clientX-c)/(d-ir);e.onUpdateAlpha(ar(A))}function l(){var n;fe("mousemove",document,o),fe("mouseup",document,l),(n=e.onComplete)===null||n===void 0||n.call(e)}return{railRef:t,railBackgroundImage:P(()=>{const{rgba:n}=e;return n?`linear-gradient(to right, rgba(${n[0]}, ${n[1]}, ${n[2]}, 0) 0%, rgba(${n[0]}, ${n[1]}, ${n[2]}, 1) 100%)`:""}),handleMouseDown:a}},render(){const{clsPrefix:e}=this;return i("div",{class:`${e}-color-picker-slider`,ref:"railRef",style:{height:ve,borderRadius:oe},onMousedown:this.handleMouseDown},i("div",{style:{borderRadius:oe,position:"absolute",left:0,right:0,top:0,bottom:0,overflow:"hidden"}},i("div",{class:`${e}-color-picker-checkboard`}),i("div",{class:`${e}-color-picker-slider__image`,style:{backgroundImage:this.railBackgroundImage}})),this.rgba&&i("div",{style:{position:"absolute",left:oe,right:oe,top:0,bottom:0}},i("div",{class:`${e}-color-picker-handle`,style:{left:`calc(${this.alpha*100}% - ${oe})`,borderRadius:oe,width:ve,height:ve}},i("div",{class:`${e}-color-picker-handle__fill`,style:{backgroundColor:X(this.rgba),borderRadius:oe,width:ve,height:ve}}))))}}),Ee=xt("n-color-picker");function ur(e){return/^\d{1,3}\.?\d*$/.test(e.trim())?Math.max(0,Math.min(Number.parseInt(e),255)):!1}function dr(e){return/^\d{1,3}\.?\d*$/.test(e.trim())?Math.max(0,Math.min(Number.parseInt(e),360)):!1}function cr(e){return/^\d{1,3}\.?\d*$/.test(e.trim())?Math.max(0,Math.min(Number.parseInt(e),100)):!1}function pr(e){const t=e.trim();return/^#[0-9a-fA-F]+$/.test(t)?[4,5,7,9].includes(t.length):!1}function hr(e){return/^\d{1,3}\.?\d*%$/.test(e.trim())?Math.max(0,Math.min(Number.parseInt(e)/100,100)):!1}const fr={paddingSmall:"0 4px"},Ge=N({name:"ColorInputUnit",props:{label:{type:String,required:!0},value:{type:[Number,String],default:null},showAlpha:Boolean,onUpdateValue:{type:Function,required:!0}},setup(e){const t=C(""),{themeRef:a}=Ze(Ee,null);We(()=>{t.value=o()});function o(){const{value:u}=e;if(u===null)return"";const{label:d}=e;return d==="HEX"?u:d==="A"?`${Math.floor(u*100)}%`:String(Math.floor(u))}function l(u){t.value=u}function n(u){let d,c;switch(e.label){case"HEX":c=pr(u),c&&e.onUpdateValue(u),t.value=o();break;case"H":d=dr(u),d===!1?t.value=o():e.onUpdateValue(d);break;case"S":case"L":case"V":d=cr(u),d===!1?t.value=o():e.onUpdateValue(d);break;case"A":d=hr(u),d===!1?t.value=o():e.onUpdateValue(d);break;case"R":case"G":case"B":d=ur(u),d===!1?t.value=o():e.onUpdateValue(d);break}}return{mergedTheme:a,inputValue:t,handleInputChange:n,handleInputUpdateValue:l}},render(){const{mergedTheme:e}=this;return i(Je,{size:"small",placeholder:this.label,theme:e.peers.Input,themeOverrides:e.peerOverrides.Input,builtinThemeOverrides:fr,value:this.inputValue,onUpdateValue:this.handleInputUpdateValue,onChange:this.handleInputChange,style:this.label==="A"?"flex-grow: 1.25;":""})}}),mr=N({name:"ColorInput",props:{clsPrefix:{type:String,required:!0},mode:{type:String,required:!0},modes:{type:Array,required:!0},showAlpha:{type:Boolean,required:!0},value:{type:String,default:null},valueArr:{type:Array,default:null},onUpdateValue:{type:Function,required:!0},onUpdateMode:{type:Function,required:!0}},setup(e){return{handleUnitUpdateValue(t,a){const{showAlpha:o}=e;if(e.mode==="hex"){e.onUpdateValue((o?Q:be)(a));return}let l;switch(e.valueArr===null?l=[0,0,0,0]:l=Array.from(e.valueArr),e.mode){case"hsv":l[t]=a,e.onUpdateValue((o?le:He)(l));break;case"rgb":l[t]=a,e.onUpdateValue((o?X:Fe)(l));break;case"hsl":l[t]=a,e.onUpdateValue((o?K:Te)(l));break}}}},render(){const{clsPrefix:e,modes:t}=this;return i("div",{class:`${e}-color-picker-input`},i("div",{class:`${e}-color-picker-input__mode`,onClick:this.onUpdateMode,style:{cursor:t.length===1?"":"pointer"}},this.mode.toUpperCase()+(this.showAlpha?"A":"")),i(Qt,null,{default:()=>{const{mode:a,valueArr:o,showAlpha:l}=this;if(a==="hex"){let n=null;try{n=o===null?null:(l?Q:be)(o)}catch{}return i(Ge,{label:"HEX",showAlpha:l,value:n,onUpdateValue:u=>{this.handleUnitUpdateValue(0,u)}})}return(a+(l?"a":"")).split("").map((n,u)=>i(Ge,{label:n.toUpperCase(),value:o===null?null:o[u],onUpdateValue:d=>{this.handleUnitUpdateValue(u,d)}}))}}))}});function gr(e,t){if(t==="hsv"){const[a,o,l,n]=ae(e);return X([...J(a,o,l),n])}return e}function vr(e){const t=document.createElement("canvas").getContext("2d");return t?(t.fillStyle=e,t.fillStyle):"#000000"}const br=N({name:"ColorPickerSwatches",props:{clsPrefix:{type:String,required:!0},mode:{type:String,required:!0},swatches:{type:Array,required:!0},onUpdateColor:{type:Function,required:!0}},setup(e){const t=P(()=>e.swatches.map(n=>{const u=xe(n);return{value:n,mode:u,legalValue:gr(n,u)}}));function a(n){const{mode:u}=e;let{value:d,mode:c}=n;return c||(c="hex",/^[a-zA-Z]+$/.test(d)?d=vr(d):(yt("color-picker",`color ${d} in swatches is invalid.`),d="#000000")),c===u?d:Qe(d,u,c)}function o(n){e.onUpdateColor(a(n))}function l(n,u){n.key==="Enter"&&o(u)}return{parsedSwatchesRef:t,handleSwatchSelect:o,handleSwatchKeyDown:l}},render(){const{clsPrefix:e}=this;return i("div",{class:`${e}-color-picker-swatches`},this.parsedSwatchesRef.map(t=>i("div",{class:`${e}-color-picker-swatch`,tabindex:0,onClick:()=>{this.handleSwatchSelect(t)},onKeydown:a=>{this.handleSwatchKeyDown(a,t)}},i("div",{class:`${e}-color-picker-swatch__fill`,style:{background:t.legalValue}}))))}}),xr=N({name:"ColorPickerTrigger",slots:Object,props:{clsPrefix:{type:String,required:!0},value:{type:String,default:null},hsla:{type:Array,default:null},disabled:Boolean,onClick:Function},setup(e){const{colorPickerSlots:t,renderLabelRef:a}=Ze(Ee,null);return()=>{const{hsla:o,value:l,clsPrefix:n,onClick:u,disabled:d}=e,c=t.label||a.value;return i("div",{class:[`${n}-color-picker`,d&&`${n}-color-picker--disabled`],onClick:d?void 0:u},i("div",{class:`${n}-color-picker__fill`},i("div",{class:`${n}-color-picker-checkboard`}),i("div",{style:{position:"absolute",left:0,right:0,top:0,bottom:0,backgroundColor:o?K(o):""}}),l&&o?i("div",{class:`${n}-color-picker__value`,style:{color:or(o)?"white":"black"}},c?c(l):l):null))}}}),yr=N({name:"ColorPreview",props:{clsPrefix:{type:String,required:!0},mode:{type:String,required:!0},color:{type:String,default:null,validator:e=>{const t=xe(e);return!!(!e||t&&t!=="hsv")}},onUpdateColor:{type:Function,required:!0}},setup(e){function t(a){var o;const l=a.target.value;(o=e.onUpdateColor)===null||o===void 0||o.call(e,Qe(l.toUpperCase(),e.mode,"hex")),a.stopPropagation()}return{handleChange:t}},render(){const{clsPrefix:e}=this;return i("div",{class:`${e}-color-picker-preview__preview`},i("span",{class:`${e}-color-picker-preview__fill`,style:{background:this.color||"#000000"}}),i("input",{class:`${e}-color-picker-preview__input`,type:"color",value:this.color,onChange:this.handleChange}))}}),ce="12px",kr=12,ne="6px",wr=6,_r="linear-gradient(90deg,red,#ff0 16.66%,#0f0 33.33%,#0ff 50%,#00f 66.66%,#f0f 83.33%,red)",Sr=N({name:"HueSlider",props:{clsPrefix:{type:String,required:!0},hue:{type:Number,required:!0},onUpdateHue:{type:Function,required:!0},onComplete:Function},setup(e){const t=C(null);function a(n){t.value&&(he("mousemove",document,o),he("mouseup",document,l),o(n))}function o(n){const{value:u}=t;if(!u)return;const{width:d,left:c}=u.getBoundingClientRect(),A=nr((n.clientX-c-wr)/(d-kr)*360);e.onUpdateHue(A)}function l(){var n;fe("mousemove",document,o),fe("mouseup",document,l),(n=e.onComplete)===null||n===void 0||n.call(e)}return{railRef:t,handleMouseDown:a}},render(){const{clsPrefix:e}=this;return i("div",{class:`${e}-color-picker-slider`,style:{height:ce,borderRadius:ne}},i("div",{ref:"railRef",style:{boxShadow:"inset 0 0 2px 0 rgba(0, 0, 0, .24)",boxSizing:"border-box",backgroundImage:_r,height:ce,borderRadius:ne,position:"relative"},onMousedown:this.handleMouseDown},i("div",{style:{position:"absolute",left:ne,right:ne,top:0,bottom:0}},i("div",{class:`${e}-color-picker-handle`,style:{left:`calc((${this.hue}%) / 359 * 100 - ${ne})`,borderRadius:ne,width:ce,height:ce}},i("div",{class:`${e}-color-picker-handle__fill`,style:{backgroundColor:`hsl(${this.hue}, 100%, 50%)`,borderRadius:ne,width:ce,height:ce}})))))}}),Ce="12px",Ue="6px",Cr=N({name:"Pallete",props:{clsPrefix:{type:String,required:!0},rgba:{type:Array,default:null},displayedHue:{type:Number,required:!0},displayedSv:{type:Array,required:!0},onUpdateSV:{type:Function,required:!0},onComplete:Function},setup(e){const t=C(null);function a(n){t.value&&(he("mousemove",document,o),he("mouseup",document,l),o(n))}function o(n){const{value:u}=t;if(!u)return;const{width:d,height:c,left:A,bottom:H}=u.getBoundingClientRect(),D=(H-n.clientY)/c,M=(n.clientX-A)/d,T=100*(M>1?1:M<0?0:M),L=100*(D>1?1:D<0?0:D);e.onUpdateSV(T,L)}function l(){var n;fe("mousemove",document,o),fe("mouseup",document,l),(n=e.onComplete)===null||n===void 0||n.call(e)}return{palleteRef:t,handleColor:P(()=>{const{rgba:n}=e;return n?`rgb(${n[0]}, ${n[1]}, ${n[2]})`:""}),handleMouseDown:a}},render(){const{clsPrefix:e}=this;return i("div",{class:`${e}-color-picker-pallete`,onMousedown:this.handleMouseDown,ref:"palleteRef"},i("div",{class:`${e}-color-picker-pallete__layer`,style:{backgroundImage:`linear-gradient(90deg, white, hsl(${this.displayedHue}, 100%, 50%))`}}),i("div",{class:`${e}-color-picker-pallete__layer ${e}-color-picker-pallete__layer--shadowed`,style:{backgroundImage:"linear-gradient(180deg, rgba(0, 0, 0, 0%), rgba(0, 0, 0, 100%))"}}),this.rgba&&i("div",{class:`${e}-color-picker-handle`,style:{width:Ce,height:Ce,borderRadius:Ue,left:`calc(${this.displayedSv[0]}% - ${Ue})`,bottom:`calc(${this.displayedSv[1]}% - ${Ue})`}},i("div",{class:`${e}-color-picker-handle__fill`,style:{backgroundColor:this.handleColor,borderRadius:Ue,width:Ce,height:Ce}})))}}),Ur=R([m("color-picker-panel",`
 margin: 4px 0;
 width: 240px;
 font-size: var(--n-panel-font-size);
 color: var(--n-text-color);
 background-color: var(--n-color);
 transition:
 box-shadow .3s var(--n-bezier),
 color .3s var(--n-bezier),
 background-color .3s var(--n-bezier);
 border-radius: var(--n-border-radius);
 box-shadow: var(--n-box-shadow);
 `,[kt(),m("input",`
 text-align: center;
 `)]),m("color-picker-checkboard",`
 background: white; 
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 `,[R("&::after",`
 background-image: linear-gradient(45deg, #DDD 25%, #0000 25%), linear-gradient(-45deg, #DDD 25%, #0000 25%), linear-gradient(45deg, #0000 75%, #DDD 75%), linear-gradient(-45deg, #0000 75%, #DDD 75%);
 background-size: 12px 12px;
 background-position: 0 0, 0 6px, 6px -6px, -6px 0px;
 background-repeat: repeat;
 content: "";
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 `)]),m("color-picker-slider",`
 margin-bottom: 8px;
 position: relative;
 box-sizing: border-box;
 `,[$("image",`
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 `),R("&::after",`
 content: "";
 position: absolute;
 border-radius: inherit;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 box-shadow: inset 0 0 2px 0 rgba(0, 0, 0, .24);
 pointer-events: none;
 `)]),m("color-picker-handle",`
 z-index: 1;
 box-shadow: 0 0 2px 0 rgba(0, 0, 0, .45);
 position: absolute;
 background-color: white;
 overflow: hidden;
 `,[$("fill",`
 box-sizing: border-box;
 border: 2px solid white;
 `)]),m("color-picker-pallete",`
 height: 180px;
 position: relative;
 margin-bottom: 8px;
 cursor: crosshair;
 `,[$("layer",`
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 `,[Ne("shadowed",`
 box-shadow: inset 0 0 2px 0 rgba(0, 0, 0, .24);
 `)])]),m("color-picker-preview",`
 display: flex;
 `,[$("sliders",`
 flex: 1 0 auto;
 `),$("preview",`
 position: relative;
 height: 30px;
 width: 30px;
 margin: 0 0 8px 6px;
 border-radius: 50%;
 box-shadow: rgba(0, 0, 0, .15) 0px 0px 0px 1px inset;
 overflow: hidden;
 `),$("fill",`
 display: block;
 width: 30px;
 height: 30px;
 `),$("input",`
 position: absolute;
 top: 0;
 left: 0;
 width: 30px;
 height: 30px;
 opacity: 0;
 z-index: 1;
 `)]),m("color-picker-input",`
 display: flex;
 align-items: center;
 `,[m("input",`
 flex-grow: 1;
 flex-basis: 0;
 `),$("mode",`
 width: 72px;
 text-align: center;
 `)]),m("color-picker-control",`
 padding: 12px;
 `),m("color-picker-action",`
 display: flex;
 margin-top: -4px;
 border-top: 1px solid var(--n-divider-color);
 padding: 8px 12px;
 justify-content: flex-end;
 `,[m("button","margin-left: 8px;")]),m("color-picker",`
 display: inline-block;
 box-sizing: border-box;
 height: var(--n-height);
 font-size: var(--n-font-size);
 width: 100%;
 position: relative;
 cursor: pointer;
 border: var(--n-border);
 border-radius: var(--n-border-radius);
 transition: border-color .3s var(--n-bezier);
 `,[Ne("disabled","cursor: not-allowed"),$("value",`
 white-space: nowrap;
 position: relative;
 `),$("fill",`
 border-radius: var(--n-border-radius);
 position: absolute;
 display: flex;
 align-items: center;
 justify-content: center;
 left: 4px;
 right: 4px;
 top: 4px;
 bottom: 4px;
 `),m("color-picker-checkboard",`
 border-radius: var(--n-border-radius);
 `,[R("&::after",`
 --n-block-size: calc((var(--n-height) - 8px) / 3);
 background-size: calc(var(--n-block-size) * 2) calc(var(--n-block-size) * 2);
 background-position: 0 0, 0 var(--n-block-size), var(--n-block-size) calc(-1 * var(--n-block-size)), calc(-1 * var(--n-block-size)) 0px; 
 `)])]),m("color-picker-swatches",`
 display: grid;
 grid-gap: 8px;
 flex-wrap: wrap;
 position: relative;
 grid-template-columns: repeat(auto-fill, 18px);
 margin-top: 10px;
 `,[m("color-picker-swatch",`
 width: 18px;
 height: 18px;
 background-image: linear-gradient(45deg, #DDD 25%, #0000 25%), linear-gradient(-45deg, #DDD 25%, #0000 25%), linear-gradient(45deg, #0000 75%, #DDD 75%), linear-gradient(-45deg, #0000 75%, #DDD 75%);
 background-size: 8px 8px;
 background-position: 0px 0, 0px 4px, 4px -4px, -4px 0px;
 background-repeat: repeat;
 `,[$("fill",`
 position: relative;
 width: 100%;
 height: 100%;
 border-radius: 3px;
 box-shadow: rgba(0, 0, 0, .15) 0px 0px 0px 1px inset;
 cursor: pointer;
 `),R("&:focus",`
 outline: none;
 `,[$("fill",[R("&::after",`
 position: absolute;
 top: 0;
 right: 0;
 bottom: 0;
 left: 0;
 background: inherit;
 filter: blur(2px);
 content: "";
 `)])])])])]),$r=Object.assign(Object.assign({},Ye.props),{value:String,show:{type:Boolean,default:void 0},defaultShow:Boolean,defaultValue:String,modes:{type:Array,default:()=>["rgb","hex","hsl"]},placement:{type:String,default:"bottom-start"},to:qe.propTo,showAlpha:{type:Boolean,default:!0},showPreview:Boolean,swatches:Array,disabled:{type:Boolean,default:void 0},actions:{type:Array,default:null},internalActions:Array,size:String,renderLabel:Function,onComplete:Function,onConfirm:Function,onClear:Function,"onUpdate:show":[Function,Array],onUpdateShow:[Function,Array],"onUpdate:value":[Function,Array],onUpdateValue:[Function,Array]}),Rr=N({name:"ColorPicker",props:$r,slots:Object,setup(e,{slots:t}){let a=null;function o(r){a=r}let l=null;const{mergedClsPrefixRef:n,namespaceRef:u,inlineThemeDisabled:d,mergedComponentPropsRef:c}=Ke(e),A=Ut(e,{mergedSize:r=>{var h,g;const{size:x}=e;if(x)return x;const{mergedSize:y}=r||{};if(y!=null&&y.value)return y.value;const v=(g=(h=c==null?void 0:c.value)===null||h===void 0?void 0:h.ColorPicker)===null||g===void 0?void 0:g.size;return v||"medium"}}),{mergedSizeRef:H,mergedDisabledRef:D}=A,{localeRef:M}=Et("global"),T=Ye("ColorPicker","-color-picker",Ur,tr,e,n);It(Ee,{themeRef:T,renderLabelRef:Ve(e,"renderLabel"),colorPickerSlots:t});const L=C(e.defaultShow),me=je(Ve(e,"show"),L);function ie(r){const{onUpdateShow:h,"onUpdate:show":g}=e;h&&_e(h,r),g&&_e(g,r),L.value=r}const{defaultValue:se}=e,p=C(se===void 0?rr(e.modes,e.showAlpha):se),s=je(Ve(e,"value"),p),w=C([s.value]),f=C(0),O=P(()=>xe(s.value)),{modes:ee}=e,U=C(xe(s.value)||ee[0]||"rgb");function ye(){const{modes:r}=e,{value:h}=U,g=r.findIndex(x=>x===h);~g?U.value=r[(g+1)%r.length]:U.value="rgb"}let I,V,Z,W,q,b,Y,F;const ge=P(()=>{const{value:r}=s;if(!r)return null;switch(O.value){case"hsv":return ae(r);case"hsl":return[I,V,Z,F]=pe(r),[...Xe(I,V,Z),F];case"rgb":case"hex":return[q,b,Y,F]=E(r),[...Me(q,b,Y),F]}}),te=P(()=>{const{value:r}=s;if(!r)return null;switch(O.value){case"rgb":case"hex":return E(r);case"hsv":return[I,V,W,F]=ae(r),[...J(I,V,W),F];case"hsl":return[I,V,Z,F]=pe(r),[...De(I,V,Z),F]}}),Re=P(()=>{const{value:r}=s;if(!r)return null;switch(O.value){case"hsl":return pe(r);case"hsv":return[I,V,W,F]=ae(r),[...$e(I,V,W),F];case"rgb":case"hex":return[q,b,Y,F]=E(r),[...Be(q,b,Y),F]}}),et=P(()=>{switch(U.value){case"rgb":case"hex":return te.value;case"hsv":return ge.value;case"hsl":return Re.value}}),ke=C(0),Ae=C(1),ze=C([0,0]);function tt(r,h){const{value:g}=ge,x=ke.value,y=g?g[3]:1;ze.value=[r,h];const{showAlpha:v}=e;switch(U.value){case"hsv":_((v?le:He)([x,r,h,y]),"cursor");break;case"hsl":_((v?K:Te)([...$e(x,r,h),y]),"cursor");break;case"rgb":_((v?X:Fe)([...J(x,r,h),y]),"cursor");break;case"hex":_((v?Q:be)([...J(x,r,h),y]),"cursor");break}}function rt(r){ke.value=r;const{value:h}=ge;if(!h)return;const[,g,x,y]=h,{showAlpha:v}=e;switch(U.value){case"hsv":_((v?le:He)([r,g,x,y]),"cursor");break;case"rgb":_((v?X:Fe)([...J(r,g,x),y]),"cursor");break;case"hex":_((v?Q:be)([...J(r,g,x),y]),"cursor");break;case"hsl":_((v?K:Te)([...$e(r,g,x),y]),"cursor");break}}function ot(r){switch(U.value){case"hsv":[I,V,W]=ge.value,_(le([I,V,W,r]),"cursor");break;case"rgb":[q,b,Y]=te.value,_(X([q,b,Y,r]),"cursor");break;case"hex":[q,b,Y]=te.value,_(Q([q,b,Y,r]),"cursor");break;case"hsl":[I,V,Z]=Re.value,_(K([I,V,Z,r]),"cursor");break}Ae.value=r}function _(r,h){h==="cursor"?l=r:l=null;const{nTriggerFormChange:g,nTriggerFormInput:x}=A,{onUpdateValue:y,"onUpdate:value":v}=e;y&&_e(y,r),v&&_e(v,r),g(),x(),p.value=r}function nt(r){_(r,"input"),zt(ue)}function ue(r=!0){const{value:h}=s;if(h){const{nTriggerFormChange:g,nTriggerFormInput:x}=A,{onComplete:y}=e;y&&y(h);const{value:v}=w,{value:B}=f;r&&(v.splice(B+1,v.length,h),f.value=B+1),g(),x()}}function at(){const{value:r}=f;r-1<0||(_(w.value[r-1],"input"),ue(!1),f.value=r-1)}function lt(){const{value:r}=f;r<0||r+1>=w.value.length||(_(w.value[r+1],"input"),ue(!1),f.value=r+1)}function it(){_(null,"input");const{onClear:r}=e;r&&r(),ie(!1)}function st(){const{value:r}=s,{onConfirm:h}=e;h&&h(r),ie(!1)}const ut=P(()=>f.value>=1),dt=P(()=>{const{value:r}=w;return r.length>1&&f.value<r.length-1});$t(me,r=>{r||(w.value=[s.value],f.value=0)}),We(()=>{if(!(l&&l===s.value)){const{value:r}=ge;r&&(ke.value=r[0],Ae.value=r[3],ze.value=[r[1],r[2]])}l=null});const Ie=P(()=>{const{value:r}=H,{common:{cubicBezierEaseInOut:h},self:{textColor:g,color:x,panelFontSize:y,boxShadow:v,border:B,borderRadius:z,dividerColor:re,[Oe("height",r)]:pt,[Oe("fontSize",r)]:ht}}=T.value;return{"--n-bezier":h,"--n-text-color":g,"--n-color":x,"--n-panel-font-size":y,"--n-font-size":ht,"--n-box-shadow":v,"--n-border":B,"--n-border-radius":z,"--n-height":pt,"--n-divider-color":re}}),j=d?Rt("color-picker",P(()=>H.value[0]),Ie,e):void 0;function ct(){var r;const{value:h}=te,{value:g}=ke,{internalActions:x,modes:y,actions:v}=e,{value:B}=T,{value:z}=n;return i("div",{class:[`${z}-color-picker-panel`,j==null?void 0:j.themeClass.value],onDragstart:re=>{re.preventDefault()},style:d?void 0:Ie.value},i("div",{class:`${z}-color-picker-control`},i(Cr,{clsPrefix:z,rgba:h,displayedHue:g,displayedSv:ze.value,onUpdateSV:tt,onComplete:ue}),i("div",{class:`${z}-color-picker-preview`},i("div",{class:`${z}-color-picker-preview__sliders`},i(Sr,{clsPrefix:z,hue:g,onUpdateHue:rt,onComplete:ue}),e.showAlpha?i(sr,{clsPrefix:z,rgba:h,alpha:Ae.value,onUpdateAlpha:ot,onComplete:ue}):null),e.showPreview?i(yr,{clsPrefix:z,mode:U.value,color:te.value&&be(te.value),onUpdateColor:re=>{_(re,"input")}}):null),i(mr,{clsPrefix:z,showAlpha:e.showAlpha,mode:U.value,modes:y,onUpdateMode:ye,value:s.value,valueArr:et.value,onUpdateValue:nt}),((r=e.swatches)===null||r===void 0?void 0:r.length)&&i(br,{clsPrefix:z,mode:U.value,swatches:e.swatches,onUpdateColor:re=>{_(re,"input")}})),v!=null&&v.length?i("div",{class:`${z}-color-picker-action`},v.includes("confirm")&&i(G,{size:"small",onClick:st,theme:B.peers.Button,themeOverrides:B.peerOverrides.Button},{default:()=>M.value.confirm}),v.includes("clear")&&i(G,{size:"small",onClick:it,disabled:!s.value,theme:B.peers.Button,themeOverrides:B.peerOverrides.Button},{default:()=>M.value.clear})):null,t.action?i("div",{class:`${z}-color-picker-action`},{default:t.action}):x?i("div",{class:`${z}-color-picker-action`},x.includes("undo")&&i(G,{size:"small",onClick:at,disabled:!ut.value,theme:B.peers.Button,themeOverrides:B.peerOverrides.Button},{default:()=>M.value.undo}),x.includes("redo")&&i(G,{size:"small",onClick:lt,disabled:!dt.value,theme:B.peers.Button,themeOverrides:B.peerOverrides.Button},{default:()=>M.value.redo})):null)}return{mergedClsPrefix:n,namespace:u,hsla:Re,rgba:te,mergedShow:me,mergedDisabled:D,isMounted:At(),adjustedTo:qe(e),mergedValue:s,handleTriggerClick(){D.value||ie(!0)},setTriggerRef:o,handleClickOutside(r){if(a instanceof Element){if(a.contains(Le(r)))return}else if(a&&a.$el.contains(Le(r)))return;ie(!1)},renderPanel:ct,cssVars:d?void 0:Ie,themeClass:j==null?void 0:j.themeClass,onRender:j==null?void 0:j.onRender}},render(){const{mergedClsPrefix:e,onRender:t}=this;return t==null||t(),i(qt,null,{default:()=>[i(Ft,null,{default:()=>wt(this.$slots.trigger,{value:this.mergedValue,onClick:this.handleTriggerClick,ref:this.setTriggerRef},a=>a||i(xr,{clsPrefix:e,value:this.mergedValue,hsla:this.hsla,style:this.cssVars,ref:this.setTriggerRef,disabled:this.mergedDisabled,class:this.themeClass,onClick:this.mergedDisabled?void 0:this.handleTriggerClick}))}),i(Ht,{placement:this.placement,show:this.mergedShow,containerClass:this.namespace,teleportDisabled:this.adjustedTo===qe.tdkey,to:this.adjustedTo},{default:()=>i(_t,{name:"fade-in-scale-up-transition",appear:this.isMounted},{default:()=>this.mergedShow?St(this.renderPanel(),[[Ct,this.handleClickOutside,void 0,{capture:!0}]]):null})})]})}}),jr={__name:"CategoryList",setup(e){const t=Ot(),a=Nt(),o=C(null),l=C([]),n=C(!1),u=C(null),d=C(!1),c=C({name:"",type:1,parentId:0,color:"#3498db",icon:""}),A=[{label:"支出",value:1},{label:"收入",value:2}],H=P(()=>{const p=l.value.filter(s=>s.parentId===0&&s.type===c.value.type);return[{label:"顶级分类",value:0},...p.map(s=>({label:s.name,value:s.id}))]}),D=P(()=>{const p=o.value?l.value.filter(s=>s.type===o.value):l.value;return M(p)});function M(p){const s=p.filter(f=>f.parentId===0),w={};return p.forEach(f=>{w[f.id]={key:f.id,label:f.name,...f,children:[]}}),p.forEach(f=>{f.parentId!==0&&w[f.parentId]&&w[f.parentId].children.push(w[f.id])}),s.map(f=>w[f.id])}function T({option:p}){return i("div",{style:{display:"flex",alignItems:"center",gap:"8px"}},[i("span",{style:{width:"12px",height:"12px",borderRadius:"50%",background:p.color||"#ccc",display:"inline-block",flexShrink:0}}),i("span",{},p.name),p.isSystem?i(Lt,{size:"tiny",type:"info",bordered:!1,style:{marginLeft:"4px"}},{default:()=>"系统"}):null,i("div",{style:{marginLeft:"auto",display:"flex",gap:"8px"}},[i(G,{size:"small",type:"primary",onClick:s=>{s.stopPropagation(),L(p)}},{default:()=>"编辑"}),p.isSystem?null:i(G,{size:"small",type:"error",onClick:s=>{s.stopPropagation(),ie(p.id)}},{default:()=>"删除"})].filter(Boolean))])}function L(p){u.value=p.id,c.value={name:p.name,type:p.type,parentId:p.parentId,color:p.color,icon:p.icon||""},n.value=!0}async function me(){if(!c.value.name){a.error("请输入名称");return}d.value=!0;try{u.value?(await we.update(u.value,c.value),a.success("更新成功")):(await we.create(c.value),a.success("创建成功")),n.value=!1,u.value=null,se()}catch{a.error("操作失败")}finally{d.value=!1}}function ie(p){t.warning({title:"确认删除",content:"删除后不可恢复，是否继续？",positiveText:"删除",negativeText:"取消",onPositiveClick:async()=>{try{await we.remove(p),a.success("已删除"),se()}catch{a.error("删除失败")}}})}async function se(){try{let w=function(f){f.forEach(O=>{var ee;s.push(O),(ee=O.children)!=null&&ee.length&&w(O.children)})};const p=await we.tree(null),s=[];w(p.data),l.value=s}catch{a.error("加载失败")}}return Vt(se),(p,s)=>{const w=Wt,f=Zt,O=ft,ee=Je,U=Gt,ye=Kt,I=Rr,V=Xt,Z=jt,W=Bt,q=Dt;return Mt(),Pt(q,{title:"分类管理"},{"header-extra":S(()=>[k(f,{value:o.value,"onUpdate:value":s[0]||(s[0]=b=>o.value=b),size:"small",style:{"margin-right":"12px"}},{default:S(()=>[k(w,{value:null},{default:S(()=>[...s[9]||(s[9]=[de("全部",-1)])]),_:1}),k(w,{value:1},{default:S(()=>[...s[10]||(s[10]=[de("支出",-1)])]),_:1}),k(w,{value:2},{default:S(()=>[...s[11]||(s[11]=[de("收入",-1)])]),_:1})]),_:1},8,["value"]),k(Pe(G),{type:"primary",onClick:s[1]||(s[1]=b=>n.value=!0)},{default:S(()=>[...s[12]||(s[12]=[de("新增分类",-1)])]),_:1})]),default:S(()=>[k(O,{data:D.value,"block-line":"","show-line":"","render-label":T,style:{"max-height":"70vh","overflow-y":"auto"}},null,8,["data"]),k(W,{show:n.value,"onUpdate:show":s[8]||(s[8]=b=>n.value=b),preset:"card",title:u.value?"编辑分类":"新增分类",style:{width:"450px"}},{default:S(()=>[k(Z,{ref:"formRef",model:c.value,"label-placement":"left","label-width":"80"},{default:S(()=>[k(U,{label:"名称",required:""},{default:S(()=>[k(ee,{value:c.value.name,"onUpdate:value":s[2]||(s[2]=b=>c.value.name=b),placeholder:"如：餐饮"},null,8,["value"])]),_:1}),k(U,{label:"类型",required:""},{default:S(()=>[k(ye,{value:c.value.type,"onUpdate:value":s[3]||(s[3]=b=>c.value.type=b),options:A},null,8,["value"])]),_:1}),k(U,{label:"父分类"},{default:S(()=>[k(ye,{value:c.value.parentId,"onUpdate:value":s[4]||(s[4]=b=>c.value.parentId=b),options:H.value,clearable:"",placeholder:"顶级分类"},null,8,["value","options"])]),_:1}),k(U,{label:"颜色"},{default:S(()=>[k(I,{value:c.value.color,"onUpdate:value":s[5]||(s[5]=b=>c.value.color=b),"show-alpha":!1},null,8,["value"])]),_:1}),k(U,{label:"图标"},{default:S(()=>[k(ee,{value:c.value.icon,"onUpdate:value":s[6]||(s[6]=b=>c.value.icon=b),placeholder:"图标标识"},null,8,["value"])]),_:1}),k(U,null,{default:S(()=>[k(V,null,{default:S(()=>[k(Pe(G),{type:"primary",loading:d.value,onClick:me},{default:S(()=>[...s[13]||(s[13]=[de("保存",-1)])]),_:1},8,["loading"]),k(Pe(G),{onClick:s[7]||(s[7]=b=>n.value=!1)},{default:S(()=>[...s[14]||(s[14]=[de("取消",-1)])]),_:1})]),_:1})]),_:1})]),_:1},8,["model"])]),_:1},8,["show","title"])]),_:1})}}};export{jr as default};
