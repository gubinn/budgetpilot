import{g as c,o as x,n as v,d as M,h as l,u as qe,af as st,c as ut,as as dt,a as ct,at as W,au as H,av as be,aw as T,ax as O,ay as L,az as oe,aA as J,aB as Ee,aC as Ue,aD as D,aE as Ae,aF as $e,m as $,r as R,aG as ne,aH as ae,f as ht,q as Ne,D as Oe,aI as se,aJ as Re,aK as ze,aL as Ve,aM as pt,al as ft,i as Ie,aN as gt,ae as mt,aj as bt,ak as vt,am as xt,j as Le,a1 as kt,l as wt,ar as Be,ao as yt,t as he,W as pe,aq as St,v as Ce,ab as Fe,p as Ct}from"./index-CSko7gqC.js";import{i as Ut,_ as At}from"./Input-DV-0Q1Ce.js";import{V as $t,a as Rt,u as _e,B as zt}from"./Popover-CPXf_MKa.js";import{b as Vt}from"./request-BL2gmoEt.js";import{u as He}from"./get-BbTDVNYA.js";const _t=c("input-group",`
 display: inline-flex;
 width: 100%;
 flex-wrap: nowrap;
 vertical-align: bottom;
`,[x(">",[c("input",[x("&:not(:last-child)",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `),x("&:not(:first-child)",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 margin-left: -1px!important;
 `)]),c("button",[x("&:not(:last-child)",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `,[v("state-border, border",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `)]),x("&:not(:first-child)",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `,[v("state-border, border",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `)])]),x("*",[x("&:not(:last-child)",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `,[x(">",[c("input",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `),c("base-selection",[c("base-selection-label",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `),c("base-selection-tags",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `),v("box-shadow, border, state-border",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `)])])]),x("&:not(:first-child)",`
 margin-left: -1px!important;
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `,[x(">",[c("input",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `),c("base-selection",[c("base-selection-label",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `),c("base-selection-tags",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `),v("box-shadow, border, state-border",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `)])])])])])]),Pt={},Dt=M({name:"InputGroup",props:Pt,setup(e){const{mergedClsPrefixRef:t}=qe(e);return st("-input-group",_t,t),{mergedClsPrefix:t}},render(){const{mergedClsPrefix:e}=this;return l("div",{class:`${e}-input-group`},this.$slots)}});function Mt(e){const{fontSize:t,boxShadow2:i,popoverColor:o,textColor2:a,borderRadius:n,borderColor:s,heightSmall:u,heightMedium:f,heightLarge:w,fontSizeSmall:I,fontSizeMedium:z,fontSizeLarge:V,dividerColor:_}=e;return{panelFontSize:t,boxShadow:i,color:o,textColor:a,borderRadius:n,border:`1px solid ${s}`,heightSmall:u,heightMedium:f,heightLarge:w,fontSizeSmall:I,fontSizeMedium:z,fontSizeLarge:V,dividerColor:_}}const It=ut({name:"ColorPicker",common:ct,peers:{Input:Ut,Button:dt},self:Mt});function Bt(e,t){switch(e[0]){case"hex":return t?"#000000FF":"#000000";case"rgb":return t?"rgba(0, 0, 0, 1)":"rgb(0, 0, 0)";case"hsl":return t?"hsla(0, 0%, 0%, 1)":"hsl(0, 0%, 0%)";case"hsv":return t?"hsva(0, 0%, 0%, 1)":"hsv(0, 0%, 0%)"}return"#000000"}function ue(e){return e===null?null:/^ *#/.test(e)?"hex":e.includes("rgb")?"rgb":e.includes("hsl")?"hsl":e.includes("hsv")?"hsv":null}function Ft(e,t=[255,255,255],i="AA"){const[o,a,n,s]=D(H(e));if(s===1){const _=fe([o,a,n]),j=fe(t);return(Math.max(_,j)+.05)/(Math.min(_,j)+.05)>=(i==="AA"?4.5:7)}const u=Math.round(o*s+t[0]*(1-s)),f=Math.round(a*s+t[1]*(1-s)),w=Math.round(n*s+t[2]*(1-s)),I=fe([u,f,w]),z=fe(t);return(Math.max(I,z)+.05)/(Math.min(I,z)+.05)>=(i==="AA"?4.5:7)}function fe(e){const[t,i,o]=e.map(a=>(a/=255,a<=.03928?a/12.92:Math.pow((a+.055)/1.055,2.4)));return .2126*t+.7152*i+.0722*o}function Ht(e){return e=Math.round(e),e>=360?359:e<0?0:e}function Tt(e){return e=Math.round(e*100)/100,e>1?1:e<0?0:e}const qt={rgb:{hex(e){return L(D(e))},hsl(e){const[t,i,o,a]=D(e);return H([...$e(t,i,o),a])},hsv(e){const[t,i,o,a]=D(e);return J([...Ae(t,i,o),a])}},hex:{rgb(e){return T(D(e))},hsl(e){const[t,i,o,a]=D(e);return H([...$e(t,i,o),a])},hsv(e){const[t,i,o,a]=D(e);return J([...Ae(t,i,o),a])}},hsl:{hex(e){const[t,i,o,a]=oe(e);return L([...Ue(t,i,o),a])},rgb(e){const[t,i,o,a]=oe(e);return T([...Ue(t,i,o),a])},hsv(e){const[t,i,o,a]=oe(e);return J([...Ee(t,i,o),a])}},hsv:{hex(e){const[t,i,o,a]=W(e);return L([...O(t,i,o),a])},rgb(e){const[t,i,o,a]=W(e);return T([...O(t,i,o),a])},hsl(e){const[t,i,o,a]=W(e);return H([...be(t,i,o),a])}}};function je(e,t,i){return i=i||ue(e),i?i===t?e:qt[i][t](e):null}const le="12px",Et=12,X="6px",Nt=M({name:"AlphaSlider",props:{clsPrefix:{type:String,required:!0},rgba:{type:Array,default:null},alpha:{type:Number,default:0},onUpdateAlpha:{type:Function,required:!0},onComplete:Function},setup(e){const t=R(null);function i(n){!t.value||!e.rgba||(ne("mousemove",document,o),ne("mouseup",document,a),o(n))}function o(n){const{value:s}=t;if(!s)return;const{width:u,left:f}=s.getBoundingClientRect(),w=(n.clientX-f)/(u-Et);e.onUpdateAlpha(Tt(w))}function a(){var n;ae("mousemove",document,o),ae("mouseup",document,a),(n=e.onComplete)===null||n===void 0||n.call(e)}return{railRef:t,railBackgroundImage:$(()=>{const{rgba:n}=e;return n?`linear-gradient(to right, rgba(${n[0]}, ${n[1]}, ${n[2]}, 0) 0%, rgba(${n[0]}, ${n[1]}, ${n[2]}, 1) 100%)`:""}),handleMouseDown:i}},render(){const{clsPrefix:e}=this;return l("div",{class:`${e}-color-picker-slider`,ref:"railRef",style:{height:le,borderRadius:X},onMousedown:this.handleMouseDown},l("div",{style:{borderRadius:X,position:"absolute",left:0,right:0,top:0,bottom:0,overflow:"hidden"}},l("div",{class:`${e}-color-picker-checkboard`}),l("div",{class:`${e}-color-picker-slider__image`,style:{backgroundImage:this.railBackgroundImage}})),this.rgba&&l("div",{style:{position:"absolute",left:X,right:X,top:0,bottom:0}},l("div",{class:`${e}-color-picker-handle`,style:{left:`calc(${this.alpha*100}% - ${X})`,borderRadius:X,width:le,height:le}},l("div",{class:`${e}-color-picker-handle__fill`,style:{backgroundColor:T(this.rgba),borderRadius:X,width:le,height:le}}))))}}),Pe=ht("n-color-picker");function Ot(e){return/^\d{1,3}\.?\d*$/.test(e.trim())?Math.max(0,Math.min(Number.parseInt(e),255)):!1}function Lt(e){return/^\d{1,3}\.?\d*$/.test(e.trim())?Math.max(0,Math.min(Number.parseInt(e),360)):!1}function jt(e){return/^\d{1,3}\.?\d*$/.test(e.trim())?Math.max(0,Math.min(Number.parseInt(e),100)):!1}function Gt(e){const t=e.trim();return/^#[0-9a-fA-F]+$/.test(t)?[4,5,7,9].includes(t.length):!1}function Kt(e){return/^\d{1,3}\.?\d*%$/.test(e.trim())?Math.max(0,Math.min(Number.parseInt(e)/100,100)):!1}const Xt={paddingSmall:"0 4px"},Te=M({name:"ColorInputUnit",props:{label:{type:String,required:!0},value:{type:[Number,String],default:null},showAlpha:Boolean,onUpdateValue:{type:Function,required:!0}},setup(e){const t=R(""),{themeRef:i}=Ne(Pe,null);Oe(()=>{t.value=o()});function o(){const{value:s}=e;if(s===null)return"";const{label:u}=e;return u==="HEX"?s:u==="A"?`${Math.floor(s*100)}%`:String(Math.floor(s))}function a(s){t.value=s}function n(s){let u,f;switch(e.label){case"HEX":f=Gt(s),f&&e.onUpdateValue(s),t.value=o();break;case"H":u=Lt(s),u===!1?t.value=o():e.onUpdateValue(u);break;case"S":case"L":case"V":u=jt(s),u===!1?t.value=o():e.onUpdateValue(u);break;case"A":u=Kt(s),u===!1?t.value=o():e.onUpdateValue(u);break;case"R":case"G":case"B":u=Ot(s),u===!1?t.value=o():e.onUpdateValue(u);break}}return{mergedTheme:i,inputValue:t,handleInputChange:n,handleInputUpdateValue:a}},render(){const{mergedTheme:e}=this;return l(At,{size:"small",placeholder:this.label,theme:e.peers.Input,themeOverrides:e.peerOverrides.Input,builtinThemeOverrides:Xt,value:this.inputValue,onUpdateValue:this.handleInputUpdateValue,onChange:this.handleInputChange,style:this.label==="A"?"flex-grow: 1.25;":""})}}),Zt=M({name:"ColorInput",props:{clsPrefix:{type:String,required:!0},mode:{type:String,required:!0},modes:{type:Array,required:!0},showAlpha:{type:Boolean,required:!0},value:{type:String,default:null},valueArr:{type:Array,default:null},onUpdateValue:{type:Function,required:!0},onUpdateMode:{type:Function,required:!0}},setup(e){return{handleUnitUpdateValue(t,i){const{showAlpha:o}=e;if(e.mode==="hex"){e.onUpdateValue((o?L:se)(i));return}let a;switch(e.valueArr===null?a=[0,0,0,0]:a=Array.from(e.valueArr),e.mode){case"hsv":a[t]=i,e.onUpdateValue((o?J:Ve)(a));break;case"rgb":a[t]=i,e.onUpdateValue((o?T:ze)(a));break;case"hsl":a[t]=i,e.onUpdateValue((o?H:Re)(a));break}}}},render(){const{clsPrefix:e,modes:t}=this;return l("div",{class:`${e}-color-picker-input`},l("div",{class:`${e}-color-picker-input__mode`,onClick:this.onUpdateMode,style:{cursor:t.length===1?"":"pointer"}},this.mode.toUpperCase()+(this.showAlpha?"A":"")),l(Dt,null,{default:()=>{const{mode:i,valueArr:o,showAlpha:a}=this;if(i==="hex"){let n=null;try{n=o===null?null:(a?L:se)(o)}catch{}return l(Te,{label:"HEX",showAlpha:a,value:n,onUpdateValue:s=>{this.handleUnitUpdateValue(0,s)}})}return(i+(a?"a":"")).split("").map((n,s)=>l(Te,{label:n.toUpperCase(),value:o===null?null:o[s],onUpdateValue:u=>{this.handleUnitUpdateValue(s,u)}}))}}))}});function Wt(e,t){if(t==="hsv"){const[i,o,a,n]=W(e);return T([...O(i,o,a),n])}return e}function Jt(e){const t=document.createElement("canvas").getContext("2d");return t?(t.fillStyle=e,t.fillStyle):"#000000"}const Yt=M({name:"ColorPickerSwatches",props:{clsPrefix:{type:String,required:!0},mode:{type:String,required:!0},swatches:{type:Array,required:!0},onUpdateColor:{type:Function,required:!0}},setup(e){const t=$(()=>e.swatches.map(n=>{const s=ue(n);return{value:n,mode:s,legalValue:Wt(n,s)}}));function i(n){const{mode:s}=e;let{value:u,mode:f}=n;return f||(f="hex",/^[a-zA-Z]+$/.test(u)?u=Jt(u):(pt("color-picker",`color ${u} in swatches is invalid.`),u="#000000")),f===s?u:je(u,s,f)}function o(n){e.onUpdateColor(i(n))}function a(n,s){n.key==="Enter"&&o(s)}return{parsedSwatchesRef:t,handleSwatchSelect:o,handleSwatchKeyDown:a}},render(){const{clsPrefix:e}=this;return l("div",{class:`${e}-color-picker-swatches`},this.parsedSwatchesRef.map(t=>l("div",{class:`${e}-color-picker-swatch`,tabindex:0,onClick:()=>{this.handleSwatchSelect(t)},onKeydown:i=>{this.handleSwatchKeyDown(i,t)}},l("div",{class:`${e}-color-picker-swatch__fill`,style:{background:t.legalValue}}))))}}),Qt=M({name:"ColorPickerTrigger",slots:Object,props:{clsPrefix:{type:String,required:!0},value:{type:String,default:null},hsla:{type:Array,default:null},disabled:Boolean,onClick:Function},setup(e){const{colorPickerSlots:t,renderLabelRef:i}=Ne(Pe,null);return()=>{const{hsla:o,value:a,clsPrefix:n,onClick:s,disabled:u}=e,f=t.label||i.value;return l("div",{class:[`${n}-color-picker`,u&&`${n}-color-picker--disabled`],onClick:u?void 0:s},l("div",{class:`${n}-color-picker__fill`},l("div",{class:`${n}-color-picker-checkboard`}),l("div",{style:{position:"absolute",left:0,right:0,top:0,bottom:0,backgroundColor:o?H(o):""}}),a&&o?l("div",{class:`${n}-color-picker__value`,style:{color:Ft(o)?"white":"black"}},f?f(a):a):null))}}}),er=M({name:"ColorPreview",props:{clsPrefix:{type:String,required:!0},mode:{type:String,required:!0},color:{type:String,default:null,validator:e=>{const t=ue(e);return!!(!e||t&&t!=="hsv")}},onUpdateColor:{type:Function,required:!0}},setup(e){function t(i){var o;const a=i.target.value;(o=e.onUpdateColor)===null||o===void 0||o.call(e,je(a.toUpperCase(),e.mode,"hex")),i.stopPropagation()}return{handleChange:t}},render(){const{clsPrefix:e}=this;return l("div",{class:`${e}-color-picker-preview__preview`},l("span",{class:`${e}-color-picker-preview__fill`,style:{background:this.color||"#000000"}}),l("input",{class:`${e}-color-picker-preview__input`,type:"color",value:this.color,onChange:this.handleChange}))}}),re="12px",tr=12,Z="6px",rr=6,or="linear-gradient(90deg,red,#ff0 16.66%,#0f0 33.33%,#0ff 50%,#00f 66.66%,#f0f 83.33%,red)",nr=M({name:"HueSlider",props:{clsPrefix:{type:String,required:!0},hue:{type:Number,required:!0},onUpdateHue:{type:Function,required:!0},onComplete:Function},setup(e){const t=R(null);function i(n){t.value&&(ne("mousemove",document,o),ne("mouseup",document,a),o(n))}function o(n){const{value:s}=t;if(!s)return;const{width:u,left:f}=s.getBoundingClientRect(),w=Ht((n.clientX-f-rr)/(u-tr)*360);e.onUpdateHue(w)}function a(){var n;ae("mousemove",document,o),ae("mouseup",document,a),(n=e.onComplete)===null||n===void 0||n.call(e)}return{railRef:t,handleMouseDown:i}},render(){const{clsPrefix:e}=this;return l("div",{class:`${e}-color-picker-slider`,style:{height:re,borderRadius:Z}},l("div",{ref:"railRef",style:{boxShadow:"inset 0 0 2px 0 rgba(0, 0, 0, .24)",boxSizing:"border-box",backgroundImage:or,height:re,borderRadius:Z,position:"relative"},onMousedown:this.handleMouseDown},l("div",{style:{position:"absolute",left:Z,right:Z,top:0,bottom:0}},l("div",{class:`${e}-color-picker-handle`,style:{left:`calc((${this.hue}%) / 359 * 100 - ${Z})`,borderRadius:Z,width:re,height:re}},l("div",{class:`${e}-color-picker-handle__fill`,style:{backgroundColor:`hsl(${this.hue}, 100%, 50%)`,borderRadius:Z,width:re,height:re}})))))}}),ge="12px",me="6px",ar=M({name:"Pallete",props:{clsPrefix:{type:String,required:!0},rgba:{type:Array,default:null},displayedHue:{type:Number,required:!0},displayedSv:{type:Array,required:!0},onUpdateSV:{type:Function,required:!0},onComplete:Function},setup(e){const t=R(null);function i(n){t.value&&(ne("mousemove",document,o),ne("mouseup",document,a),o(n))}function o(n){const{value:s}=t;if(!s)return;const{width:u,height:f,left:w,bottom:I}=s.getBoundingClientRect(),z=(I-n.clientY)/f,V=(n.clientX-w)/u,_=100*(V>1?1:V<0?0:V),j=100*(z>1?1:z<0?0:z);e.onUpdateSV(_,j)}function a(){var n;ae("mousemove",document,o),ae("mouseup",document,a),(n=e.onComplete)===null||n===void 0||n.call(e)}return{palleteRef:t,handleColor:$(()=>{const{rgba:n}=e;return n?`rgb(${n[0]}, ${n[1]}, ${n[2]})`:""}),handleMouseDown:i}},render(){const{clsPrefix:e}=this;return l("div",{class:`${e}-color-picker-pallete`,onMousedown:this.handleMouseDown,ref:"palleteRef"},l("div",{class:`${e}-color-picker-pallete__layer`,style:{backgroundImage:`linear-gradient(90deg, white, hsl(${this.displayedHue}, 100%, 50%))`}}),l("div",{class:`${e}-color-picker-pallete__layer ${e}-color-picker-pallete__layer--shadowed`,style:{backgroundImage:"linear-gradient(180deg, rgba(0, 0, 0, 0%), rgba(0, 0, 0, 100%))"}}),this.rgba&&l("div",{class:`${e}-color-picker-handle`,style:{width:ge,height:ge,borderRadius:me,left:`calc(${this.displayedSv[0]}% - ${me})`,bottom:`calc(${this.displayedSv[1]}% - ${me})`}},l("div",{class:`${e}-color-picker-handle__fill`,style:{backgroundColor:this.handleColor,borderRadius:me,width:ge,height:ge}})))}}),ir=x([c("color-picker-panel",`
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
 `,[ft(),c("input",`
 text-align: center;
 `)]),c("color-picker-checkboard",`
 background: white; 
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 `,[x("&::after",`
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
 `)]),c("color-picker-slider",`
 margin-bottom: 8px;
 position: relative;
 box-sizing: border-box;
 `,[v("image",`
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 `),x("&::after",`
 content: "";
 position: absolute;
 border-radius: inherit;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 box-shadow: inset 0 0 2px 0 rgba(0, 0, 0, .24);
 pointer-events: none;
 `)]),c("color-picker-handle",`
 z-index: 1;
 box-shadow: 0 0 2px 0 rgba(0, 0, 0, .45);
 position: absolute;
 background-color: white;
 overflow: hidden;
 `,[v("fill",`
 box-sizing: border-box;
 border: 2px solid white;
 `)]),c("color-picker-pallete",`
 height: 180px;
 position: relative;
 margin-bottom: 8px;
 cursor: crosshair;
 `,[v("layer",`
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 `,[Ie("shadowed",`
 box-shadow: inset 0 0 2px 0 rgba(0, 0, 0, .24);
 `)])]),c("color-picker-preview",`
 display: flex;
 `,[v("sliders",`
 flex: 1 0 auto;
 `),v("preview",`
 position: relative;
 height: 30px;
 width: 30px;
 margin: 0 0 8px 6px;
 border-radius: 50%;
 box-shadow: rgba(0, 0, 0, .15) 0px 0px 0px 1px inset;
 overflow: hidden;
 `),v("fill",`
 display: block;
 width: 30px;
 height: 30px;
 `),v("input",`
 position: absolute;
 top: 0;
 left: 0;
 width: 30px;
 height: 30px;
 opacity: 0;
 z-index: 1;
 `)]),c("color-picker-input",`
 display: flex;
 align-items: center;
 `,[c("input",`
 flex-grow: 1;
 flex-basis: 0;
 `),v("mode",`
 width: 72px;
 text-align: center;
 `)]),c("color-picker-control",`
 padding: 12px;
 `),c("color-picker-action",`
 display: flex;
 margin-top: -4px;
 border-top: 1px solid var(--n-divider-color);
 padding: 8px 12px;
 justify-content: flex-end;
 `,[c("button","margin-left: 8px;")]),c("color-picker",`
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
 `,[Ie("disabled","cursor: not-allowed"),v("value",`
 white-space: nowrap;
 position: relative;
 `),v("fill",`
 border-radius: var(--n-border-radius);
 position: absolute;
 display: flex;
 align-items: center;
 justify-content: center;
 left: 4px;
 right: 4px;
 top: 4px;
 bottom: 4px;
 `),c("color-picker-checkboard",`
 border-radius: var(--n-border-radius);
 `,[x("&::after",`
 --n-block-size: calc((var(--n-height) - 8px) / 3);
 background-size: calc(var(--n-block-size) * 2) calc(var(--n-block-size) * 2);
 background-position: 0 0, 0 var(--n-block-size), var(--n-block-size) calc(-1 * var(--n-block-size)), calc(-1 * var(--n-block-size)) 0px; 
 `)])]),c("color-picker-swatches",`
 display: grid;
 grid-gap: 8px;
 flex-wrap: wrap;
 position: relative;
 grid-template-columns: repeat(auto-fill, 18px);
 margin-top: 10px;
 `,[c("color-picker-swatch",`
 width: 18px;
 height: 18px;
 background-image: linear-gradient(45deg, #DDD 25%, #0000 25%), linear-gradient(-45deg, #DDD 25%, #0000 25%), linear-gradient(45deg, #0000 75%, #DDD 75%), linear-gradient(-45deg, #0000 75%, #DDD 75%);
 background-size: 8px 8px;
 background-position: 0px 0, 0px 4px, 4px -4px, -4px 0px;
 background-repeat: repeat;
 `,[v("fill",`
 position: relative;
 width: 100%;
 height: 100%;
 border-radius: 3px;
 box-shadow: rgba(0, 0, 0, .15) 0px 0px 0px 1px inset;
 cursor: pointer;
 `),x("&:focus",`
 outline: none;
 `,[v("fill",[x("&::after",`
 position: absolute;
 top: 0;
 right: 0;
 bottom: 0;
 left: 0;
 background: inherit;
 filter: blur(2px);
 content: "";
 `)])])])])]),lr=Object.assign(Object.assign({},Le.props),{value:String,show:{type:Boolean,default:void 0},defaultShow:Boolean,defaultValue:String,modes:{type:Array,default:()=>["rgb","hex","hsl"]},placement:{type:String,default:"bottom-start"},to:_e.propTo,showAlpha:{type:Boolean,default:!0},showPreview:Boolean,swatches:Array,disabled:{type:Boolean,default:void 0},actions:{type:Array,default:null},internalActions:Array,size:String,renderLabel:Function,onComplete:Function,onConfirm:Function,onClear:Function,"onUpdate:show":[Function,Array],onUpdateShow:[Function,Array],"onUpdate:value":[Function,Array],onUpdateValue:[Function,Array]}),pr=M({name:"ColorPicker",props:lr,slots:Object,setup(e,{slots:t}){let i=null;function o(r){i=r}let a=null;const{mergedClsPrefixRef:n,namespaceRef:s,inlineThemeDisabled:u,mergedComponentPropsRef:f}=qe(e),w=xt(e,{mergedSize:r=>{var d,h;const{size:g}=e;if(g)return g;const{mergedSize:m}=r||{};if(m!=null&&m.value)return m.value;const p=(h=(d=f==null?void 0:f.value)===null||d===void 0?void 0:d.ColorPicker)===null||h===void 0?void 0:h.size;return p||"medium"}}),{mergedSizeRef:I,mergedDisabledRef:z}=w,{localeRef:V}=Vt("global"),_=Le("ColorPicker","-color-picker",ir,It,e,n);Ct(Pe,{themeRef:_,renderLabelRef:Ce(e,"renderLabel"),colorPickerSlots:t});const j=R(e.defaultShow),ve=He(Ce(e,"show"),j);function de(r){const{onUpdateShow:d,"onUpdate:show":h}=e;d&&he(d,r),h&&he(h,r),j.value=r}const{defaultValue:De}=e,Me=R(De===void 0?Bt(e.modes,e.showAlpha):De),y=He(Ce(e,"value"),Me),Y=R([y.value]),B=R(0),xe=$(()=>ue(y.value)),{modes:Ge}=e,P=R(ue(y.value)||Ge[0]||"rgb");function Ke(){const{modes:r}=e,{value:d}=P,h=r.findIndex(g=>g===d);~h?P.value=r[(h+1)%r.length]:P.value="rgb"}let C,U,Q,ee,q,E,N,A;const ie=$(()=>{const{value:r}=y;if(!r)return null;switch(xe.value){case"hsv":return W(r);case"hsl":return[C,U,Q,A]=oe(r),[...Ee(C,U,Q),A];case"rgb":case"hex":return[q,E,N,A]=D(r),[...Ae(q,E,N),A]}}),G=$(()=>{const{value:r}=y;if(!r)return null;switch(xe.value){case"rgb":case"hex":return D(r);case"hsv":return[C,U,ee,A]=W(r),[...O(C,U,ee),A];case"hsl":return[C,U,Q,A]=oe(r),[...Ue(C,U,Q),A]}}),ke=$(()=>{const{value:r}=y;if(!r)return null;switch(xe.value){case"hsl":return oe(r);case"hsv":return[C,U,ee,A]=W(r),[...be(C,U,ee),A];case"rgb":case"hex":return[q,E,N,A]=D(r),[...$e(q,E,N),A]}}),Xe=$(()=>{switch(P.value){case"rgb":case"hex":return G.value;case"hsv":return ie.value;case"hsl":return ke.value}}),ce=R(0),we=R(1),ye=R([0,0]);function Ze(r,d){const{value:h}=ie,g=ce.value,m=h?h[3]:1;ye.value=[r,d];const{showAlpha:p}=e;switch(P.value){case"hsv":b((p?J:Ve)([g,r,d,m]),"cursor");break;case"hsl":b((p?H:Re)([...be(g,r,d),m]),"cursor");break;case"rgb":b((p?T:ze)([...O(g,r,d),m]),"cursor");break;case"hex":b((p?L:se)([...O(g,r,d),m]),"cursor");break}}function We(r){ce.value=r;const{value:d}=ie;if(!d)return;const[,h,g,m]=d,{showAlpha:p}=e;switch(P.value){case"hsv":b((p?J:Ve)([r,h,g,m]),"cursor");break;case"rgb":b((p?T:ze)([...O(r,h,g),m]),"cursor");break;case"hex":b((p?L:se)([...O(r,h,g),m]),"cursor");break;case"hsl":b((p?H:Re)([...be(r,h,g),m]),"cursor");break}}function Je(r){switch(P.value){case"hsv":[C,U,ee]=ie.value,b(J([C,U,ee,r]),"cursor");break;case"rgb":[q,E,N]=G.value,b(T([q,E,N,r]),"cursor");break;case"hex":[q,E,N]=G.value,b(L([q,E,N,r]),"cursor");break;case"hsl":[C,U,Q]=ke.value,b(H([C,U,Q,r]),"cursor");break}we.value=r}function b(r,d){d==="cursor"?a=r:a=null;const{nTriggerFormChange:h,nTriggerFormInput:g}=w,{onUpdateValue:m,"onUpdate:value":p}=e;m&&he(m,r),p&&he(p,r),h(),g(),Me.value=r}function Ye(r){b(r,"input"),St(te)}function te(r=!0){const{value:d}=y;if(d){const{nTriggerFormChange:h,nTriggerFormInput:g}=w,{onComplete:m}=e;m&&m(d);const{value:p}=Y,{value:S}=B;r&&(p.splice(S+1,p.length,d),B.value=S+1),h(),g()}}function Qe(){const{value:r}=B;r-1<0||(b(Y.value[r-1],"input"),te(!1),B.value=r-1)}function et(){const{value:r}=B;r<0||r+1>=Y.value.length||(b(Y.value[r+1],"input"),te(!1),B.value=r+1)}function tt(){b(null,"input");const{onClear:r}=e;r&&r(),de(!1)}function rt(){const{value:r}=y,{onConfirm:d}=e;d&&d(r),de(!1)}const ot=$(()=>B.value>=1),nt=$(()=>{const{value:r}=Y;return r.length>1&&B.value<r.length-1});kt(ve,r=>{r||(Y.value=[y.value],B.value=0)}),Oe(()=>{if(!(a&&a===y.value)){const{value:r}=ie;r&&(ce.value=r[0],we.value=r[3],ye.value=[r[1],r[2]])}a=null});const Se=$(()=>{const{value:r}=I,{common:{cubicBezierEaseInOut:d},self:{textColor:h,color:g,panelFontSize:m,boxShadow:p,border:S,borderRadius:k,dividerColor:K,[Fe("height",r)]:it,[Fe("fontSize",r)]:lt}}=_.value;return{"--n-bezier":d,"--n-text-color":h,"--n-color":g,"--n-panel-font-size":m,"--n-font-size":lt,"--n-box-shadow":p,"--n-border":S,"--n-border-radius":k,"--n-height":it,"--n-divider-color":K}}),F=u?wt("color-picker",$(()=>I.value[0]),Se,e):void 0;function at(){var r;const{value:d}=G,{value:h}=ce,{internalActions:g,modes:m,actions:p}=e,{value:S}=_,{value:k}=n;return l("div",{class:[`${k}-color-picker-panel`,F==null?void 0:F.themeClass.value],onDragstart:K=>{K.preventDefault()},style:u?void 0:Se.value},l("div",{class:`${k}-color-picker-control`},l(ar,{clsPrefix:k,rgba:d,displayedHue:h,displayedSv:ye.value,onUpdateSV:Ze,onComplete:te}),l("div",{class:`${k}-color-picker-preview`},l("div",{class:`${k}-color-picker-preview__sliders`},l(nr,{clsPrefix:k,hue:h,onUpdateHue:We,onComplete:te}),e.showAlpha?l(Nt,{clsPrefix:k,rgba:d,alpha:we.value,onUpdateAlpha:Je,onComplete:te}):null),e.showPreview?l(er,{clsPrefix:k,mode:P.value,color:G.value&&se(G.value),onUpdateColor:K=>{b(K,"input")}}):null),l(Zt,{clsPrefix:k,showAlpha:e.showAlpha,mode:P.value,modes:m,onUpdateMode:Ke,value:y.value,valueArr:Xe.value,onUpdateValue:Ye}),((r=e.swatches)===null||r===void 0?void 0:r.length)&&l(Yt,{clsPrefix:k,mode:P.value,swatches:e.swatches,onUpdateColor:K=>{b(K,"input")}})),p!=null&&p.length?l("div",{class:`${k}-color-picker-action`},p.includes("confirm")&&l(pe,{size:"small",onClick:rt,theme:S.peers.Button,themeOverrides:S.peerOverrides.Button},{default:()=>V.value.confirm}),p.includes("clear")&&l(pe,{size:"small",onClick:tt,disabled:!y.value,theme:S.peers.Button,themeOverrides:S.peerOverrides.Button},{default:()=>V.value.clear})):null,t.action?l("div",{class:`${k}-color-picker-action`},{default:t.action}):g?l("div",{class:`${k}-color-picker-action`},g.includes("undo")&&l(pe,{size:"small",onClick:Qe,disabled:!ot.value,theme:S.peers.Button,themeOverrides:S.peerOverrides.Button},{default:()=>V.value.undo}),g.includes("redo")&&l(pe,{size:"small",onClick:et,disabled:!nt.value,theme:S.peers.Button,themeOverrides:S.peerOverrides.Button},{default:()=>V.value.redo})):null)}return{mergedClsPrefix:n,namespace:s,hsla:ke,rgba:G,mergedShow:ve,mergedDisabled:z,isMounted:yt(),adjustedTo:_e(e),mergedValue:y,handleTriggerClick(){z.value||de(!0)},setTriggerRef:o,handleClickOutside(r){if(i instanceof Element){if(i.contains(Be(r)))return}else if(i&&i.$el.contains(Be(r)))return;de(!1)},renderPanel:at,cssVars:u?void 0:Se,themeClass:F==null?void 0:F.themeClass,onRender:F==null?void 0:F.onRender}},render(){const{mergedClsPrefix:e,onRender:t}=this;return t==null||t(),l(zt,null,{default:()=>[l($t,null,{default:()=>gt(this.$slots.trigger,{value:this.mergedValue,onClick:this.handleTriggerClick,ref:this.setTriggerRef},i=>i||l(Qt,{clsPrefix:e,value:this.mergedValue,hsla:this.hsla,style:this.cssVars,ref:this.setTriggerRef,disabled:this.mergedDisabled,class:this.themeClass,onClick:this.mergedDisabled?void 0:this.handleTriggerClick}))}),l(Rt,{placement:this.placement,show:this.mergedShow,containerClass:this.namespace,teleportDisabled:this.adjustedTo===_e.tdkey,to:this.adjustedTo},{default:()=>l(mt,{name:"fade-in-scale-up-transition",appear:this.isMounted},{default:()=>this.mergedShow?bt(this.renderPanel(),[[vt,this.handleClickOutside,void 0,{capture:!0}]]):null})})]})}});export{pr as _};
