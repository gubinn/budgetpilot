import{l as it,a9 as st,m as ut,au as Z,av as B,aw as me,ax as F,ay as N,az as O,aA as ne,aB as W,aC as qe,aD as Ue,aE as D,aF as Ae,aG as Re,d as H,k as i,C as A,j as R,aH as oe,aI as ae,q as ct,G as Ee,T as Ne,aJ as se,aK as $e,aL as _e,aM as ze,aN as dt,F as te,t as m,an as ht,E as U,v as Ie,aO as pt,ah as ft,al as gt,am as vt,x as mt,ao as bt,y as Oe,a4 as xt,A as kt,at as Be,aq as wt,H as he,B as pe,as as yt,I as Ce,aP as Fe,D as St}from"./index-DO5RWIzg.js";import{i as Ct,_ as Ut}from"./Input-Bh1MgBrd.js";import{_ as At}from"./InputGroup-BXmF5jKQ.js";import{V as Rt,a as $t,u as Ve,B as _t}from"./Popover-BrGH2xK_.js";import{a as zt}from"./use-message-DwYPNKPJ.js";import{u as He}from"./get-BzPm1k_J.js";function Vt(e){const{fontSize:t,boxShadow2:l,popoverColor:n,textColor2:a,borderRadius:o,borderColor:s,heightSmall:u,heightMedium:p,heightLarge:x,fontSizeSmall:M,fontSizeMedium:$,fontSizeLarge:_,dividerColor:z}=e;return{panelFontSize:t,boxShadow:l,color:n,textColor:a,borderRadius:o,border:`1px solid ${s}`,heightSmall:u,heightMedium:p,heightLarge:x,fontSizeSmall:M,fontSizeMedium:$,fontSizeLarge:_,dividerColor:z}}const Dt=it({name:"ColorPicker",common:ut,peers:{Input:Ct,Button:st},self:Vt});function Mt(e,t){switch(e[0]){case"hex":return t?"#000000FF":"#000000";case"rgb":return t?"rgba(0, 0, 0, 1)":"rgb(0, 0, 0)";case"hsl":return t?"hsla(0, 0%, 0%, 1)":"hsl(0, 0%, 0%)";case"hsv":return t?"hsva(0, 0%, 0%, 1)":"hsv(0, 0%, 0%)"}return"#000000"}function ue(e){return e===null?null:/^ *#/.test(e)?"hex":e.includes("rgb")?"rgb":e.includes("hsl")?"hsl":e.includes("hsv")?"hsv":null}function Pt(e,t=[255,255,255],l="AA"){const[n,a,o,s]=D(B(e));if(s===1){const z=fe([n,a,o]),L=fe(t);return(Math.max(z,L)+.05)/(Math.min(z,L)+.05)>=(l==="AA"?4.5:7)}const u=Math.round(n*s+t[0]*(1-s)),p=Math.round(a*s+t[1]*(1-s)),x=Math.round(o*s+t[2]*(1-s)),M=fe([u,p,x]),$=fe(t);return(Math.max(M,$)+.05)/(Math.min(M,$)+.05)>=(l==="AA"?4.5:7)}function fe(e){const[t,l,n]=e.map(a=>(a/=255,a<=.03928?a/12.92:Math.pow((a+.055)/1.055,2.4)));return .2126*t+.7152*l+.0722*n}function It(e){return e=Math.round(e),e>=360?359:e<0?0:e}function Bt(e){return e=Math.round(e*100)/100,e>1?1:e<0?0:e}const Ft={rgb:{hex(e){return O(D(e))},hsl(e){const[t,l,n,a]=D(e);return B([...Re(t,l,n),a])},hsv(e){const[t,l,n,a]=D(e);return W([...Ae(t,l,n),a])}},hex:{rgb(e){return F(D(e))},hsl(e){const[t,l,n,a]=D(e);return B([...Re(t,l,n),a])},hsv(e){const[t,l,n,a]=D(e);return W([...Ae(t,l,n),a])}},hsl:{hex(e){const[t,l,n,a]=ne(e);return O([...Ue(t,l,n),a])},rgb(e){const[t,l,n,a]=ne(e);return F([...Ue(t,l,n),a])},hsv(e){const[t,l,n,a]=ne(e);return W([...qe(t,l,n),a])}},hsv:{hex(e){const[t,l,n,a]=Z(e);return O([...N(t,l,n),a])},rgb(e){const[t,l,n,a]=Z(e);return F([...N(t,l,n),a])},hsl(e){const[t,l,n,a]=Z(e);return B([...me(t,l,n),a])}}};function Le(e,t,l){return l=l||ue(e),l?l===t?e:Ft[l][t](e):null}const ie="12px",Ht=12,G="6px",Tt=H({name:"AlphaSlider",props:{clsPrefix:{type:String,required:!0},rgba:{type:Array,default:null},alpha:{type:Number,default:0},onUpdateAlpha:{type:Function,required:!0},onComplete:Function},setup(e){const t=R(null);function l(o){!t.value||!e.rgba||(oe("mousemove",document,n),oe("mouseup",document,a),n(o))}function n(o){const{value:s}=t;if(!s)return;const{width:u,left:p}=s.getBoundingClientRect(),x=(o.clientX-p)/(u-Ht);e.onUpdateAlpha(Bt(x))}function a(){var o;ae("mousemove",document,n),ae("mouseup",document,a),(o=e.onComplete)===null||o===void 0||o.call(e)}return{railRef:t,railBackgroundImage:A(()=>{const{rgba:o}=e;return o?`linear-gradient(to right, rgba(${o[0]}, ${o[1]}, ${o[2]}, 0) 0%, rgba(${o[0]}, ${o[1]}, ${o[2]}, 1) 100%)`:""}),handleMouseDown:l}},render(){const{clsPrefix:e}=this;return i("div",{class:`${e}-color-picker-slider`,ref:"railRef",style:{height:ie,borderRadius:G},onMousedown:this.handleMouseDown},i("div",{style:{borderRadius:G,position:"absolute",left:0,right:0,top:0,bottom:0,overflow:"hidden"}},i("div",{class:`${e}-color-picker-checkboard`}),i("div",{class:`${e}-color-picker-slider__image`,style:{backgroundImage:this.railBackgroundImage}})),this.rgba&&i("div",{style:{position:"absolute",left:G,right:G,top:0,bottom:0}},i("div",{class:`${e}-color-picker-handle`,style:{left:`calc(${this.alpha*100}% - ${G})`,borderRadius:G,width:ie,height:ie}},i("div",{class:`${e}-color-picker-handle__fill`,style:{backgroundColor:F(this.rgba),borderRadius:G,width:ie,height:ie}}))))}}),De=ct("n-color-picker");function qt(e){return/^\d{1,3}\.?\d*$/.test(e.trim())?Math.max(0,Math.min(Number.parseInt(e),255)):!1}function Et(e){return/^\d{1,3}\.?\d*$/.test(e.trim())?Math.max(0,Math.min(Number.parseInt(e),360)):!1}function Nt(e){return/^\d{1,3}\.?\d*$/.test(e.trim())?Math.max(0,Math.min(Number.parseInt(e),100)):!1}function Ot(e){const t=e.trim();return/^#[0-9a-fA-F]+$/.test(t)?[4,5,7,9].includes(t.length):!1}function Lt(e){return/^\d{1,3}\.?\d*%$/.test(e.trim())?Math.max(0,Math.min(Number.parseInt(e)/100,100)):!1}const jt={paddingSmall:"0 4px"},Te=H({name:"ColorInputUnit",props:{label:{type:String,required:!0},value:{type:[Number,String],default:null},showAlpha:Boolean,onUpdateValue:{type:Function,required:!0}},setup(e){const t=R(""),{themeRef:l}=Ee(De,null);Ne(()=>{t.value=n()});function n(){const{value:s}=e;if(s===null)return"";const{label:u}=e;return u==="HEX"?s:u==="A"?`${Math.floor(s*100)}%`:String(Math.floor(s))}function a(s){t.value=s}function o(s){let u,p;switch(e.label){case"HEX":p=Ot(s),p&&e.onUpdateValue(s),t.value=n();break;case"H":u=Et(s),u===!1?t.value=n():e.onUpdateValue(u);break;case"S":case"L":case"V":u=Nt(s),u===!1?t.value=n():e.onUpdateValue(u);break;case"A":u=Lt(s),u===!1?t.value=n():e.onUpdateValue(u);break;case"R":case"G":case"B":u=qt(s),u===!1?t.value=n():e.onUpdateValue(u);break}}return{mergedTheme:l,inputValue:t,handleInputChange:o,handleInputUpdateValue:a}},render(){const{mergedTheme:e}=this;return i(Ut,{size:"small",placeholder:this.label,theme:e.peers.Input,themeOverrides:e.peerOverrides.Input,builtinThemeOverrides:jt,value:this.inputValue,onUpdateValue:this.handleInputUpdateValue,onChange:this.handleInputChange,style:this.label==="A"?"flex-grow: 1.25;":""})}}),Kt=H({name:"ColorInput",props:{clsPrefix:{type:String,required:!0},mode:{type:String,required:!0},modes:{type:Array,required:!0},showAlpha:{type:Boolean,required:!0},value:{type:String,default:null},valueArr:{type:Array,default:null},onUpdateValue:{type:Function,required:!0},onUpdateMode:{type:Function,required:!0}},setup(e){return{handleUnitUpdateValue(t,l){const{showAlpha:n}=e;if(e.mode==="hex"){e.onUpdateValue((n?O:se)(l));return}let a;switch(e.valueArr===null?a=[0,0,0,0]:a=Array.from(e.valueArr),e.mode){case"hsv":a[t]=l,e.onUpdateValue((n?W:ze)(a));break;case"rgb":a[t]=l,e.onUpdateValue((n?F:_e)(a));break;case"hsl":a[t]=l,e.onUpdateValue((n?B:$e)(a));break}}}},render(){const{clsPrefix:e,modes:t}=this;return i("div",{class:`${e}-color-picker-input`},i("div",{class:`${e}-color-picker-input__mode`,onClick:this.onUpdateMode,style:{cursor:t.length===1?"":"pointer"}},this.mode.toUpperCase()+(this.showAlpha?"A":"")),i(At,null,{default:()=>{const{mode:l,valueArr:n,showAlpha:a}=this;if(l==="hex"){let o=null;try{o=n===null?null:(a?O:se)(n)}catch{}return i(Te,{label:"HEX",showAlpha:a,value:o,onUpdateValue:s=>{this.handleUnitUpdateValue(0,s)}})}return(l+(a?"a":"")).split("").map((o,s)=>i(Te,{label:o.toUpperCase(),value:n===null?null:n[s],onUpdateValue:u=>{this.handleUnitUpdateValue(s,u)}}))}}))}});function Gt(e,t){if(t==="hsv"){const[l,n,a,o]=Z(e);return F([...N(l,n,a),o])}return e}function Xt(e){const t=document.createElement("canvas").getContext("2d");return t?(t.fillStyle=e,t.fillStyle):"#000000"}const Zt=H({name:"ColorPickerSwatches",props:{clsPrefix:{type:String,required:!0},mode:{type:String,required:!0},swatches:{type:Array,required:!0},onUpdateColor:{type:Function,required:!0}},setup(e){const t=A(()=>e.swatches.map(o=>{const s=ue(o);return{value:o,mode:s,legalValue:Gt(o,s)}}));function l(o){const{mode:s}=e;let{value:u,mode:p}=o;return p||(p="hex",/^[a-zA-Z]+$/.test(u)?u=Xt(u):(dt("color-picker",`color ${u} in swatches is invalid.`),u="#000000")),p===s?u:Le(u,s,p)}function n(o){e.onUpdateColor(l(o))}function a(o,s){o.key==="Enter"&&n(s)}return{parsedSwatchesRef:t,handleSwatchSelect:n,handleSwatchKeyDown:a}},render(){const{clsPrefix:e}=this;return i("div",{class:`${e}-color-picker-swatches`},this.parsedSwatchesRef.map(t=>i("div",{class:`${e}-color-picker-swatch`,tabindex:0,onClick:()=>{this.handleSwatchSelect(t)},onKeydown:l=>{this.handleSwatchKeyDown(l,t)}},i("div",{class:`${e}-color-picker-swatch__fill`,style:{background:t.legalValue}}))))}}),Wt=H({name:"ColorPickerTrigger",slots:Object,props:{clsPrefix:{type:String,required:!0},value:{type:String,default:null},hsla:{type:Array,default:null},disabled:Boolean,onClick:Function},setup(e){const{colorPickerSlots:t,renderLabelRef:l}=Ee(De,null);return()=>{const{hsla:n,value:a,clsPrefix:o,onClick:s,disabled:u}=e,p=t.label||l.value;return i("div",{class:[`${o}-color-picker`,u&&`${o}-color-picker--disabled`],onClick:u?void 0:s},i("div",{class:`${o}-color-picker__fill`},i("div",{class:`${o}-color-picker-checkboard`}),i("div",{style:{position:"absolute",left:0,right:0,top:0,bottom:0,backgroundColor:n?B(n):""}}),a&&n?i("div",{class:`${o}-color-picker__value`,style:{color:Pt(n)?"white":"black"}},p?p(a):a):null))}}}),Jt=H({name:"ColorPreview",props:{clsPrefix:{type:String,required:!0},mode:{type:String,required:!0},color:{type:String,default:null,validator:e=>{const t=ue(e);return!!(!e||t&&t!=="hsv")}},onUpdateColor:{type:Function,required:!0}},setup(e){function t(l){var n;const a=l.target.value;(n=e.onUpdateColor)===null||n===void 0||n.call(e,Le(a.toUpperCase(),e.mode,"hex")),l.stopPropagation()}return{handleChange:t}},render(){const{clsPrefix:e}=this;return i("div",{class:`${e}-color-picker-preview__preview`},i("span",{class:`${e}-color-picker-preview__fill`,style:{background:this.color||"#000000"}}),i("input",{class:`${e}-color-picker-preview__input`,type:"color",value:this.color,onChange:this.handleChange}))}}),re="12px",Yt=12,X="6px",Qt=6,er="linear-gradient(90deg,red,#ff0 16.66%,#0f0 33.33%,#0ff 50%,#00f 66.66%,#f0f 83.33%,red)",tr=H({name:"HueSlider",props:{clsPrefix:{type:String,required:!0},hue:{type:Number,required:!0},onUpdateHue:{type:Function,required:!0},onComplete:Function},setup(e){const t=R(null);function l(o){t.value&&(oe("mousemove",document,n),oe("mouseup",document,a),n(o))}function n(o){const{value:s}=t;if(!s)return;const{width:u,left:p}=s.getBoundingClientRect(),x=It((o.clientX-p-Qt)/(u-Yt)*360);e.onUpdateHue(x)}function a(){var o;ae("mousemove",document,n),ae("mouseup",document,a),(o=e.onComplete)===null||o===void 0||o.call(e)}return{railRef:t,handleMouseDown:l}},render(){const{clsPrefix:e}=this;return i("div",{class:`${e}-color-picker-slider`,style:{height:re,borderRadius:X}},i("div",{ref:"railRef",style:{boxShadow:"inset 0 0 2px 0 rgba(0, 0, 0, .24)",boxSizing:"border-box",backgroundImage:er,height:re,borderRadius:X,position:"relative"},onMousedown:this.handleMouseDown},i("div",{style:{position:"absolute",left:X,right:X,top:0,bottom:0}},i("div",{class:`${e}-color-picker-handle`,style:{left:`calc((${this.hue}%) / 359 * 100 - ${X})`,borderRadius:X,width:re,height:re}},i("div",{class:`${e}-color-picker-handle__fill`,style:{backgroundColor:`hsl(${this.hue}, 100%, 50%)`,borderRadius:X,width:re,height:re}})))))}}),ge="12px",ve="6px",rr=H({name:"Pallete",props:{clsPrefix:{type:String,required:!0},rgba:{type:Array,default:null},displayedHue:{type:Number,required:!0},displayedSv:{type:Array,required:!0},onUpdateSV:{type:Function,required:!0},onComplete:Function},setup(e){const t=R(null);function l(o){t.value&&(oe("mousemove",document,n),oe("mouseup",document,a),n(o))}function n(o){const{value:s}=t;if(!s)return;const{width:u,height:p,left:x,bottom:M}=s.getBoundingClientRect(),$=(M-o.clientY)/p,_=(o.clientX-x)/u,z=100*(_>1?1:_<0?0:_),L=100*($>1?1:$<0?0:$);e.onUpdateSV(z,L)}function a(){var o;ae("mousemove",document,n),ae("mouseup",document,a),(o=e.onComplete)===null||o===void 0||o.call(e)}return{palleteRef:t,handleColor:A(()=>{const{rgba:o}=e;return o?`rgb(${o[0]}, ${o[1]}, ${o[2]})`:""}),handleMouseDown:l}},render(){const{clsPrefix:e}=this;return i("div",{class:`${e}-color-picker-pallete`,onMousedown:this.handleMouseDown,ref:"palleteRef"},i("div",{class:`${e}-color-picker-pallete__layer`,style:{backgroundImage:`linear-gradient(90deg, white, hsl(${this.displayedHue}, 100%, 50%))`}}),i("div",{class:`${e}-color-picker-pallete__layer ${e}-color-picker-pallete__layer--shadowed`,style:{backgroundImage:"linear-gradient(180deg, rgba(0, 0, 0, 0%), rgba(0, 0, 0, 100%))"}}),this.rgba&&i("div",{class:`${e}-color-picker-handle`,style:{width:ge,height:ge,borderRadius:ve,left:`calc(${this.displayedSv[0]}% - ${ve})`,bottom:`calc(${this.displayedSv[1]}% - ${ve})`}},i("div",{class:`${e}-color-picker-handle__fill`,style:{backgroundColor:this.handleColor,borderRadius:ve,width:ge,height:ge}})))}}),nr=te([m("color-picker-panel",`
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
 `,[ht(),m("input",`
 text-align: center;
 `)]),m("color-picker-checkboard",`
 background: white; 
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 `,[te("&::after",`
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
 `,[U("image",`
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 `),te("&::after",`
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
 `,[U("fill",`
 box-sizing: border-box;
 border: 2px solid white;
 `)]),m("color-picker-pallete",`
 height: 180px;
 position: relative;
 margin-bottom: 8px;
 cursor: crosshair;
 `,[U("layer",`
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 `,[Ie("shadowed",`
 box-shadow: inset 0 0 2px 0 rgba(0, 0, 0, .24);
 `)])]),m("color-picker-preview",`
 display: flex;
 `,[U("sliders",`
 flex: 1 0 auto;
 `),U("preview",`
 position: relative;
 height: 30px;
 width: 30px;
 margin: 0 0 8px 6px;
 border-radius: 50%;
 box-shadow: rgba(0, 0, 0, .15) 0px 0px 0px 1px inset;
 overflow: hidden;
 `),U("fill",`
 display: block;
 width: 30px;
 height: 30px;
 `),U("input",`
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
 `),U("mode",`
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
 `,[Ie("disabled","cursor: not-allowed"),U("value",`
 white-space: nowrap;
 position: relative;
 `),U("fill",`
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
 `,[te("&::after",`
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
 `,[U("fill",`
 position: relative;
 width: 100%;
 height: 100%;
 border-radius: 3px;
 box-shadow: rgba(0, 0, 0, .15) 0px 0px 0px 1px inset;
 cursor: pointer;
 `),te("&:focus",`
 outline: none;
 `,[U("fill",[te("&::after",`
 position: absolute;
 top: 0;
 right: 0;
 bottom: 0;
 left: 0;
 background: inherit;
 filter: blur(2px);
 content: "";
 `)])])])])]),or=Object.assign(Object.assign({},Oe.props),{value:String,show:{type:Boolean,default:void 0},defaultShow:Boolean,defaultValue:String,modes:{type:Array,default:()=>["rgb","hex","hsl"]},placement:{type:String,default:"bottom-start"},to:Ve.propTo,showAlpha:{type:Boolean,default:!0},showPreview:Boolean,swatches:Array,disabled:{type:Boolean,default:void 0},actions:{type:Array,default:null},internalActions:Array,size:String,renderLabel:Function,onComplete:Function,onConfirm:Function,onClear:Function,"onUpdate:show":[Function,Array],onUpdateShow:[Function,Array],"onUpdate:value":[Function,Array],onUpdateValue:[Function,Array]}),dr=H({name:"ColorPicker",props:or,slots:Object,setup(e,{slots:t}){let l=null;function n(r){l=r}let a=null;const{mergedClsPrefixRef:o,namespaceRef:s,inlineThemeDisabled:u,mergedComponentPropsRef:p}=mt(e),x=bt(e,{mergedSize:r=>{var c,d;const{size:f}=e;if(f)return f;const{mergedSize:g}=r||{};if(g!=null&&g.value)return g.value;const h=(d=(c=p==null?void 0:p.value)===null||c===void 0?void 0:c.ColorPicker)===null||d===void 0?void 0:d.size;return h||"medium"}}),{mergedSizeRef:M,mergedDisabledRef:$}=x,{localeRef:_}=zt("global"),z=Oe("ColorPicker","-color-picker",nr,Dt,e,o);St(De,{themeRef:z,renderLabelRef:Ce(e,"renderLabel"),colorPickerSlots:t});const L=R(e.defaultShow),be=He(Ce(e,"show"),L);function ce(r){const{onUpdateShow:c,"onUpdate:show":d}=e;c&&he(c,r),d&&he(d,r),L.value=r}const{defaultValue:Me}=e,Pe=R(Me===void 0?Mt(e.modes,e.showAlpha):Me),k=He(Ce(e,"value"),Pe),J=R([k.value]),P=R(0),xe=A(()=>ue(k.value)),{modes:je}=e,V=R(ue(k.value)||je[0]||"rgb");function Ke(){const{modes:r}=e,{value:c}=V,d=r.findIndex(f=>f===c);~d?V.value=r[(d+1)%r.length]:V.value="rgb"}let y,S,Y,Q,T,q,E,C;const le=A(()=>{const{value:r}=k;if(!r)return null;switch(xe.value){case"hsv":return Z(r);case"hsl":return[y,S,Y,C]=ne(r),[...qe(y,S,Y),C];case"rgb":case"hex":return[T,q,E,C]=D(r),[...Ae(T,q,E),C]}}),j=A(()=>{const{value:r}=k;if(!r)return null;switch(xe.value){case"rgb":case"hex":return D(r);case"hsv":return[y,S,Q,C]=Z(r),[...N(y,S,Q),C];case"hsl":return[y,S,Y,C]=ne(r),[...Ue(y,S,Y),C]}}),ke=A(()=>{const{value:r}=k;if(!r)return null;switch(xe.value){case"hsl":return ne(r);case"hsv":return[y,S,Q,C]=Z(r),[...me(y,S,Q),C];case"rgb":case"hex":return[T,q,E,C]=D(r),[...Re(T,q,E),C]}}),Ge=A(()=>{switch(V.value){case"rgb":case"hex":return j.value;case"hsv":return le.value;case"hsl":return ke.value}}),de=R(0),we=R(1),ye=R([0,0]);function Xe(r,c){const{value:d}=le,f=de.value,g=d?d[3]:1;ye.value=[r,c];const{showAlpha:h}=e;switch(V.value){case"hsv":v((h?W:ze)([f,r,c,g]),"cursor");break;case"hsl":v((h?B:$e)([...me(f,r,c),g]),"cursor");break;case"rgb":v((h?F:_e)([...N(f,r,c),g]),"cursor");break;case"hex":v((h?O:se)([...N(f,r,c),g]),"cursor");break}}function Ze(r){de.value=r;const{value:c}=le;if(!c)return;const[,d,f,g]=c,{showAlpha:h}=e;switch(V.value){case"hsv":v((h?W:ze)([r,d,f,g]),"cursor");break;case"rgb":v((h?F:_e)([...N(r,d,f),g]),"cursor");break;case"hex":v((h?O:se)([...N(r,d,f),g]),"cursor");break;case"hsl":v((h?B:$e)([...me(r,d,f),g]),"cursor");break}}function We(r){switch(V.value){case"hsv":[y,S,Q]=le.value,v(W([y,S,Q,r]),"cursor");break;case"rgb":[T,q,E]=j.value,v(F([T,q,E,r]),"cursor");break;case"hex":[T,q,E]=j.value,v(O([T,q,E,r]),"cursor");break;case"hsl":[y,S,Y]=ke.value,v(B([y,S,Y,r]),"cursor");break}we.value=r}function v(r,c){c==="cursor"?a=r:a=null;const{nTriggerFormChange:d,nTriggerFormInput:f}=x,{onUpdateValue:g,"onUpdate:value":h}=e;g&&he(g,r),h&&he(h,r),d(),f(),Pe.value=r}function Je(r){v(r,"input"),yt(ee)}function ee(r=!0){const{value:c}=k;if(c){const{nTriggerFormChange:d,nTriggerFormInput:f}=x,{onComplete:g}=e;g&&g(c);const{value:h}=J,{value:w}=P;r&&(h.splice(w+1,h.length,c),P.value=w+1),d(),f()}}function Ye(){const{value:r}=P;r-1<0||(v(J.value[r-1],"input"),ee(!1),P.value=r-1)}function Qe(){const{value:r}=P;r<0||r+1>=J.value.length||(v(J.value[r+1],"input"),ee(!1),P.value=r+1)}function et(){v(null,"input");const{onClear:r}=e;r&&r(),ce(!1)}function tt(){const{value:r}=k,{onConfirm:c}=e;c&&c(r),ce(!1)}const rt=A(()=>P.value>=1),nt=A(()=>{const{value:r}=J;return r.length>1&&P.value<r.length-1});xt(be,r=>{r||(J.value=[k.value],P.value=0)}),Ne(()=>{if(!(a&&a===k.value)){const{value:r}=le;r&&(de.value=r[0],we.value=r[3],ye.value=[r[1],r[2]])}a=null});const Se=A(()=>{const{value:r}=M,{common:{cubicBezierEaseInOut:c},self:{textColor:d,color:f,panelFontSize:g,boxShadow:h,border:w,borderRadius:b,dividerColor:K,[Fe("height",r)]:at,[Fe("fontSize",r)]:lt}}=z.value;return{"--n-bezier":c,"--n-text-color":d,"--n-color":f,"--n-panel-font-size":g,"--n-font-size":lt,"--n-box-shadow":h,"--n-border":w,"--n-border-radius":b,"--n-height":at,"--n-divider-color":K}}),I=u?kt("color-picker",A(()=>M.value[0]),Se,e):void 0;function ot(){var r;const{value:c}=j,{value:d}=de,{internalActions:f,modes:g,actions:h}=e,{value:w}=z,{value:b}=o;return i("div",{class:[`${b}-color-picker-panel`,I==null?void 0:I.themeClass.value],onDragstart:K=>{K.preventDefault()},style:u?void 0:Se.value},i("div",{class:`${b}-color-picker-control`},i(rr,{clsPrefix:b,rgba:c,displayedHue:d,displayedSv:ye.value,onUpdateSV:Xe,onComplete:ee}),i("div",{class:`${b}-color-picker-preview`},i("div",{class:`${b}-color-picker-preview__sliders`},i(tr,{clsPrefix:b,hue:d,onUpdateHue:Ze,onComplete:ee}),e.showAlpha?i(Tt,{clsPrefix:b,rgba:c,alpha:we.value,onUpdateAlpha:We,onComplete:ee}):null),e.showPreview?i(Jt,{clsPrefix:b,mode:V.value,color:j.value&&se(j.value),onUpdateColor:K=>{v(K,"input")}}):null),i(Kt,{clsPrefix:b,showAlpha:e.showAlpha,mode:V.value,modes:g,onUpdateMode:Ke,value:k.value,valueArr:Ge.value,onUpdateValue:Je}),((r=e.swatches)===null||r===void 0?void 0:r.length)&&i(Zt,{clsPrefix:b,mode:V.value,swatches:e.swatches,onUpdateColor:K=>{v(K,"input")}})),h!=null&&h.length?i("div",{class:`${b}-color-picker-action`},h.includes("confirm")&&i(pe,{size:"small",onClick:tt,theme:w.peers.Button,themeOverrides:w.peerOverrides.Button},{default:()=>_.value.confirm}),h.includes("clear")&&i(pe,{size:"small",onClick:et,disabled:!k.value,theme:w.peers.Button,themeOverrides:w.peerOverrides.Button},{default:()=>_.value.clear})):null,t.action?i("div",{class:`${b}-color-picker-action`},{default:t.action}):f?i("div",{class:`${b}-color-picker-action`},f.includes("undo")&&i(pe,{size:"small",onClick:Ye,disabled:!rt.value,theme:w.peers.Button,themeOverrides:w.peerOverrides.Button},{default:()=>_.value.undo}),f.includes("redo")&&i(pe,{size:"small",onClick:Qe,disabled:!nt.value,theme:w.peers.Button,themeOverrides:w.peerOverrides.Button},{default:()=>_.value.redo})):null)}return{mergedClsPrefix:o,namespace:s,hsla:ke,rgba:j,mergedShow:be,mergedDisabled:$,isMounted:wt(),adjustedTo:Ve(e),mergedValue:k,handleTriggerClick(){$.value||ce(!0)},setTriggerRef:n,handleClickOutside(r){if(l instanceof Element){if(l.contains(Be(r)))return}else if(l&&l.$el.contains(Be(r)))return;ce(!1)},renderPanel:ot,cssVars:u?void 0:Se,themeClass:I==null?void 0:I.themeClass,onRender:I==null?void 0:I.onRender}},render(){const{mergedClsPrefix:e,onRender:t}=this;return t==null||t(),i(_t,null,{default:()=>[i(Rt,null,{default:()=>pt(this.$slots.trigger,{value:this.mergedValue,onClick:this.handleTriggerClick,ref:this.setTriggerRef},l=>l||i(Wt,{clsPrefix:e,value:this.mergedValue,hsla:this.hsla,style:this.cssVars,ref:this.setTriggerRef,disabled:this.mergedDisabled,class:this.themeClass,onClick:this.mergedDisabled?void 0:this.handleTriggerClick}))}),i($t,{placement:this.placement,show:this.mergedShow,containerClass:this.namespace,teleportDisabled:this.adjustedTo===Ve.tdkey,to:this.adjustedTo},{default:()=>i(ft,{name:"fade-in-scale-up-transition",appear:this.isMounted},{default:()=>this.mergedShow?gt(this.renderPanel(),[[vt,this.handleClickOutside,void 0,{capture:!0}]]):null})})]})}});export{dr as _};
