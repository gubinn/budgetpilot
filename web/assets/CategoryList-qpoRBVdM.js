import{c as we}from"./index-zhuMgjIl.js";import{_ as ht}from"./Tree-CGnzy4Le.js";import{e as m,a as $,i as U,d as O,h as i,p as Ge,u as ft,B as mt,am as gt,j as vt,an as oe,ao as K,ap as $e,aq as Z,ar as Y,as as Q,at as pe,au as ae,av as Ke,aw as Pe,ax as L,ay as De,az as Me,c as V,r as C,aA as he,aB as fe,F as bt,J as Ze,W as Xe,aC as xe,aD as Be,aE as Te,aF as Fe,aG as xt,k as yt,g as Ee,aH as kt,l as wt,aI as _t,aJ as St,aK as Ct,q as We,w as Ut,x as $t,aL as Ne,aM as At,K as _e,a7 as be,n as Rt,t as Ve,z as Le,H as zt,s as It,a0 as Vt,a1 as S,aa as Pt,Z as Dt,a5 as k,al as Mt,ad as de}from"./index-MrwMms4Y.js";import{i as Bt,_ as Je}from"./Input-D2gqu1iv.js";import{a as Tt,b as Ft,u as He,B as Ht}from"./Popover-BYWPPD8Y.js";import{a as qt,u as Et}from"./request-CsxWwiuF.js";import{u as Oe}from"./get-DeXD5z1Q.js";import{u as Nt}from"./composables-Dl26EkIt.js";import{_ as Lt,a as Ot}from"./FormItem-CUOvxB8u.js";import{_ as jt}from"./Select-DQz6Dgml.js";import{_ as Gt}from"./Space-Bj5g2v4Z.js";import{_ as Kt}from"./RadioGroup-DFPVkkjj.js";import{_ as Zt}from"./RadioButton-CXqT_O7D.js";import"./Checkbox-366QR7em.js";const Xt=m("input-group",`
 display: inline-flex;
 width: 100%;
 flex-wrap: nowrap;
 vertical-align: bottom;
`,[$(">",[m("input",[$("&:not(:last-child)",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `),$("&:not(:first-child)",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 margin-left: -1px!important;
 `)]),m("button",[$("&:not(:last-child)",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `,[U("state-border, border",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `)]),$("&:not(:first-child)",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `,[U("state-border, border",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `)])]),$("*",[$("&:not(:last-child)",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `,[$(">",[m("input",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `),m("base-selection",[m("base-selection-label",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `),m("base-selection-tags",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `),U("box-shadow, border, state-border",`
 border-top-right-radius: 0!important;
 border-bottom-right-radius: 0!important;
 `)])])]),$("&:not(:first-child)",`
 margin-left: -1px!important;
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `,[$(">",[m("input",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `),m("base-selection",[m("base-selection-label",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `),m("base-selection-tags",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `),U("box-shadow, border, state-border",`
 border-top-left-radius: 0!important;
 border-bottom-left-radius: 0!important;
 `)])])])])])]),Wt={},Jt=O({name:"InputGroup",props:Wt,setup(e){const{mergedClsPrefixRef:t}=Ge(e);return ft("-input-group",Xt,t),{mergedClsPrefix:t}},render(){const{mergedClsPrefix:e}=this;return i("div",{class:`${e}-input-group`},this.$slots)}});function Yt(e){const{fontSize:t,boxShadow2:a,popoverColor:n,textColor2:l,borderRadius:o,borderColor:u,heightSmall:d,heightMedium:c,heightLarge:A,fontSizeSmall:H,fontSizeMedium:P,fontSizeLarge:D,dividerColor:T}=e;return{panelFontSize:t,boxShadow:a,color:n,textColor:l,borderRadius:o,border:`1px solid ${u}`,heightSmall:d,heightMedium:c,heightLarge:A,fontSizeSmall:H,fontSizeMedium:P,fontSizeLarge:D,dividerColor:T}}const Qt=mt({name:"ColorPicker",common:vt,peers:{Input:Bt,Button:gt},self:Yt});function er(e,t){switch(e[0]){case"hex":return t?"#000000FF":"#000000";case"rgb":return t?"rgba(0, 0, 0, 1)":"rgb(0, 0, 0)";case"hsl":return t?"hsla(0, 0%, 0%, 1)":"hsl(0, 0%, 0%)";case"hsv":return t?"hsva(0, 0%, 0%, 1)":"hsv(0, 0%, 0%)"}return"#000000"}function ye(e){return e===null?null:/^ *#/.test(e)?"hex":e.includes("rgb")?"rgb":e.includes("hsl")?"hsl":e.includes("hsv")?"hsv":null}function tr(e,t=[255,255,255],a="AA"){const[n,l,o,u]=L(K(e));if(u===1){const T=Se([n,l,o]),j=Se(t);return(Math.max(T,j)+.05)/(Math.min(T,j)+.05)>=(a==="AA"?4.5:7)}const d=Math.round(n*u+t[0]*(1-u)),c=Math.round(l*u+t[1]*(1-u)),A=Math.round(o*u+t[2]*(1-u)),H=Se([d,c,A]),P=Se(t);return(Math.max(H,P)+.05)/(Math.min(H,P)+.05)>=(a==="AA"?4.5:7)}function Se(e){const[t,a,n]=e.map(l=>(l/=255,l<=.03928?l/12.92:Math.pow((l+.055)/1.055,2.4)));return .2126*t+.7152*a+.0722*n}function rr(e){return e=Math.round(e),e>=360?359:e<0?0:e}function nr(e){return e=Math.round(e*100)/100,e>1?1:e<0?0:e}const or={rgb:{hex(e){return Q(L(e))},hsl(e){const[t,a,n,l]=L(e);return K([...Me(t,a,n),l])},hsv(e){const[t,a,n,l]=L(e);return ae([...De(t,a,n),l])}},hex:{rgb(e){return Z(L(e))},hsl(e){const[t,a,n,l]=L(e);return K([...Me(t,a,n),l])},hsv(e){const[t,a,n,l]=L(e);return ae([...De(t,a,n),l])}},hsl:{hex(e){const[t,a,n,l]=pe(e);return Q([...Pe(t,a,n),l])},rgb(e){const[t,a,n,l]=pe(e);return Z([...Pe(t,a,n),l])},hsv(e){const[t,a,n,l]=pe(e);return ae([...Ke(t,a,n),l])}},hsv:{hex(e){const[t,a,n,l]=oe(e);return Q([...Y(t,a,n),l])},rgb(e){const[t,a,n,l]=oe(e);return Z([...Y(t,a,n),l])},hsl(e){const[t,a,n,l]=oe(e);return K([...$e(t,a,n),l])}}};function Ye(e,t,a){return a=a||ye(e),a?a===t?e:or[a][t](e):null}const ve="12px",ar=12,re="6px",lr=O({name:"AlphaSlider",props:{clsPrefix:{type:String,required:!0},rgba:{type:Array,default:null},alpha:{type:Number,default:0},onUpdateAlpha:{type:Function,required:!0},onComplete:Function},setup(e){const t=C(null);function a(o){!t.value||!e.rgba||(he("mousemove",document,n),he("mouseup",document,l),n(o))}function n(o){const{value:u}=t;if(!u)return;const{width:d,left:c}=u.getBoundingClientRect(),A=(o.clientX-c)/(d-ar);e.onUpdateAlpha(nr(A))}function l(){var o;fe("mousemove",document,n),fe("mouseup",document,l),(o=e.onComplete)===null||o===void 0||o.call(e)}return{railRef:t,railBackgroundImage:V(()=>{const{rgba:o}=e;return o?`linear-gradient(to right, rgba(${o[0]}, ${o[1]}, ${o[2]}, 0) 0%, rgba(${o[0]}, ${o[1]}, ${o[2]}, 1) 100%)`:""}),handleMouseDown:a}},render(){const{clsPrefix:e}=this;return i("div",{class:`${e}-color-picker-slider`,ref:"railRef",style:{height:ve,borderRadius:re},onMousedown:this.handleMouseDown},i("div",{style:{borderRadius:re,position:"absolute",left:0,right:0,top:0,bottom:0,overflow:"hidden"}},i("div",{class:`${e}-color-picker-checkboard`}),i("div",{class:`${e}-color-picker-slider__image`,style:{backgroundImage:this.railBackgroundImage}})),this.rgba&&i("div",{style:{position:"absolute",left:re,right:re,top:0,bottom:0}},i("div",{class:`${e}-color-picker-handle`,style:{left:`calc(${this.alpha*100}% - ${re})`,borderRadius:re,width:ve,height:ve}},i("div",{class:`${e}-color-picker-handle__fill`,style:{backgroundColor:Z(this.rgba),borderRadius:re,width:ve,height:ve}}))))}}),qe=bt("n-color-picker");function ir(e){return/^\d{1,3}\.?\d*$/.test(e.trim())?Math.max(0,Math.min(Number.parseInt(e),255)):!1}function sr(e){return/^\d{1,3}\.?\d*$/.test(e.trim())?Math.max(0,Math.min(Number.parseInt(e),360)):!1}function ur(e){return/^\d{1,3}\.?\d*$/.test(e.trim())?Math.max(0,Math.min(Number.parseInt(e),100)):!1}function dr(e){const t=e.trim();return/^#[0-9a-fA-F]+$/.test(t)?[4,5,7,9].includes(t.length):!1}function cr(e){return/^\d{1,3}\.?\d*%$/.test(e.trim())?Math.max(0,Math.min(Number.parseInt(e)/100,100)):!1}const pr={paddingSmall:"0 4px"},je=O({name:"ColorInputUnit",props:{label:{type:String,required:!0},value:{type:[Number,String],default:null},showAlpha:Boolean,onUpdateValue:{type:Function,required:!0}},setup(e){const t=C(""),{themeRef:a}=Ze(qe,null);Xe(()=>{t.value=n()});function n(){const{value:u}=e;if(u===null)return"";const{label:d}=e;return d==="HEX"?u:d==="A"?`${Math.floor(u*100)}%`:String(Math.floor(u))}function l(u){t.value=u}function o(u){let d,c;switch(e.label){case"HEX":c=dr(u),c&&e.onUpdateValue(u),t.value=n();break;case"H":d=sr(u),d===!1?t.value=n():e.onUpdateValue(d);break;case"S":case"L":case"V":d=ur(u),d===!1?t.value=n():e.onUpdateValue(d);break;case"A":d=cr(u),d===!1?t.value=n():e.onUpdateValue(d);break;case"R":case"G":case"B":d=ir(u),d===!1?t.value=n():e.onUpdateValue(d);break}}return{mergedTheme:a,inputValue:t,handleInputChange:o,handleInputUpdateValue:l}},render(){const{mergedTheme:e}=this;return i(Je,{size:"small",placeholder:this.label,theme:e.peers.Input,themeOverrides:e.peerOverrides.Input,builtinThemeOverrides:pr,value:this.inputValue,onUpdateValue:this.handleInputUpdateValue,onChange:this.handleInputChange,style:this.label==="A"?"flex-grow: 1.25;":""})}}),hr=O({name:"ColorInput",props:{clsPrefix:{type:String,required:!0},mode:{type:String,required:!0},modes:{type:Array,required:!0},showAlpha:{type:Boolean,required:!0},value:{type:String,default:null},valueArr:{type:Array,default:null},onUpdateValue:{type:Function,required:!0},onUpdateMode:{type:Function,required:!0}},setup(e){return{handleUnitUpdateValue(t,a){const{showAlpha:n}=e;if(e.mode==="hex"){e.onUpdateValue((n?Q:xe)(a));return}let l;switch(e.valueArr===null?l=[0,0,0,0]:l=Array.from(e.valueArr),e.mode){case"hsv":l[t]=a,e.onUpdateValue((n?ae:Fe)(l));break;case"rgb":l[t]=a,e.onUpdateValue((n?Z:Te)(l));break;case"hsl":l[t]=a,e.onUpdateValue((n?K:Be)(l));break}}}},render(){const{clsPrefix:e,modes:t}=this;return i("div",{class:`${e}-color-picker-input`},i("div",{class:`${e}-color-picker-input__mode`,onClick:this.onUpdateMode,style:{cursor:t.length===1?"":"pointer"}},this.mode.toUpperCase()+(this.showAlpha?"A":"")),i(Jt,null,{default:()=>{const{mode:a,valueArr:n,showAlpha:l}=this;if(a==="hex"){let o=null;try{o=n===null?null:(l?Q:xe)(n)}catch{}return i(je,{label:"HEX",showAlpha:l,value:o,onUpdateValue:u=>{this.handleUnitUpdateValue(0,u)}})}return(a+(l?"a":"")).split("").map((o,u)=>i(je,{label:o.toUpperCase(),value:n===null?null:n[u],onUpdateValue:d=>{this.handleUnitUpdateValue(u,d)}}))}}))}});function fr(e,t){if(t==="hsv"){const[a,n,l,o]=oe(e);return Z([...Y(a,n,l),o])}return e}function mr(e){const t=document.createElement("canvas").getContext("2d");return t?(t.fillStyle=e,t.fillStyle):"#000000"}const gr=O({name:"ColorPickerSwatches",props:{clsPrefix:{type:String,required:!0},mode:{type:String,required:!0},swatches:{type:Array,required:!0},onUpdateColor:{type:Function,required:!0}},setup(e){const t=V(()=>e.swatches.map(o=>{const u=ye(o);return{value:o,mode:u,legalValue:fr(o,u)}}));function a(o){const{mode:u}=e;let{value:d,mode:c}=o;return c||(c="hex",/^[a-zA-Z]+$/.test(d)?d=mr(d):(xt("color-picker",`color ${d} in swatches is invalid.`),d="#000000")),c===u?d:Ye(d,u,c)}function n(o){e.onUpdateColor(a(o))}function l(o,u){o.key==="Enter"&&n(u)}return{parsedSwatchesRef:t,handleSwatchSelect:n,handleSwatchKeyDown:l}},render(){const{clsPrefix:e}=this;return i("div",{class:`${e}-color-picker-swatches`},this.parsedSwatchesRef.map(t=>i("div",{class:`${e}-color-picker-swatch`,tabindex:0,onClick:()=>{this.handleSwatchSelect(t)},onKeydown:a=>{this.handleSwatchKeyDown(a,t)}},i("div",{class:`${e}-color-picker-swatch__fill`,style:{background:t.legalValue}}))))}}),vr=O({name:"ColorPickerTrigger",slots:Object,props:{clsPrefix:{type:String,required:!0},value:{type:String,default:null},hsla:{type:Array,default:null},disabled:Boolean,onClick:Function},setup(e){const{colorPickerSlots:t,renderLabelRef:a}=Ze(qe,null);return()=>{const{hsla:n,value:l,clsPrefix:o,onClick:u,disabled:d}=e,c=t.label||a.value;return i("div",{class:[`${o}-color-picker`,d&&`${o}-color-picker--disabled`],onClick:d?void 0:u},i("div",{class:`${o}-color-picker__fill`},i("div",{class:`${o}-color-picker-checkboard`}),i("div",{style:{position:"absolute",left:0,right:0,top:0,bottom:0,backgroundColor:n?K(n):""}}),l&&n?i("div",{class:`${o}-color-picker__value`,style:{color:tr(n)?"white":"black"}},c?c(l):l):null))}}}),br=O({name:"ColorPreview",props:{clsPrefix:{type:String,required:!0},mode:{type:String,required:!0},color:{type:String,default:null,validator:e=>{const t=ye(e);return!!(!e||t&&t!=="hsv")}},onUpdateColor:{type:Function,required:!0}},setup(e){function t(a){var n;const l=a.target.value;(n=e.onUpdateColor)===null||n===void 0||n.call(e,Ye(l.toUpperCase(),e.mode,"hex")),a.stopPropagation()}return{handleChange:t}},render(){const{clsPrefix:e}=this;return i("div",{class:`${e}-color-picker-preview__preview`},i("span",{class:`${e}-color-picker-preview__fill`,style:{background:this.color||"#000000"}}),i("input",{class:`${e}-color-picker-preview__input`,type:"color",value:this.color,onChange:this.handleChange}))}}),ce="12px",xr=12,ne="6px",yr=6,kr="linear-gradient(90deg,red,#ff0 16.66%,#0f0 33.33%,#0ff 50%,#00f 66.66%,#f0f 83.33%,red)",wr=O({name:"HueSlider",props:{clsPrefix:{type:String,required:!0},hue:{type:Number,required:!0},onUpdateHue:{type:Function,required:!0},onComplete:Function},setup(e){const t=C(null);function a(o){t.value&&(he("mousemove",document,n),he("mouseup",document,l),n(o))}function n(o){const{value:u}=t;if(!u)return;const{width:d,left:c}=u.getBoundingClientRect(),A=rr((o.clientX-c-yr)/(d-xr)*360);e.onUpdateHue(A)}function l(){var o;fe("mousemove",document,n),fe("mouseup",document,l),(o=e.onComplete)===null||o===void 0||o.call(e)}return{railRef:t,handleMouseDown:a}},render(){const{clsPrefix:e}=this;return i("div",{class:`${e}-color-picker-slider`,style:{height:ce,borderRadius:ne}},i("div",{ref:"railRef",style:{boxShadow:"inset 0 0 2px 0 rgba(0, 0, 0, .24)",boxSizing:"border-box",backgroundImage:kr,height:ce,borderRadius:ne,position:"relative"},onMousedown:this.handleMouseDown},i("div",{style:{position:"absolute",left:ne,right:ne,top:0,bottom:0}},i("div",{class:`${e}-color-picker-handle`,style:{left:`calc((${this.hue}%) / 359 * 100 - ${ne})`,borderRadius:ne,width:ce,height:ce}},i("div",{class:`${e}-color-picker-handle__fill`,style:{backgroundColor:`hsl(${this.hue}, 100%, 50%)`,borderRadius:ne,width:ce,height:ce}})))))}}),Ce="12px",Ue="6px",_r=O({name:"Pallete",props:{clsPrefix:{type:String,required:!0},rgba:{type:Array,default:null},displayedHue:{type:Number,required:!0},displayedSv:{type:Array,required:!0},onUpdateSV:{type:Function,required:!0},onComplete:Function},setup(e){const t=C(null);function a(o){t.value&&(he("mousemove",document,n),he("mouseup",document,l),n(o))}function n(o){const{value:u}=t;if(!u)return;const{width:d,height:c,left:A,bottom:H}=u.getBoundingClientRect(),P=(H-o.clientY)/c,D=(o.clientX-A)/d,T=100*(D>1?1:D<0?0:D),j=100*(P>1?1:P<0?0:P);e.onUpdateSV(T,j)}function l(){var o;fe("mousemove",document,n),fe("mouseup",document,l),(o=e.onComplete)===null||o===void 0||o.call(e)}return{palleteRef:t,handleColor:V(()=>{const{rgba:o}=e;return o?`rgb(${o[0]}, ${o[1]}, ${o[2]})`:""}),handleMouseDown:a}},render(){const{clsPrefix:e}=this;return i("div",{class:`${e}-color-picker-pallete`,onMousedown:this.handleMouseDown,ref:"palleteRef"},i("div",{class:`${e}-color-picker-pallete__layer`,style:{backgroundImage:`linear-gradient(90deg, white, hsl(${this.displayedHue}, 100%, 50%))`}}),i("div",{class:`${e}-color-picker-pallete__layer ${e}-color-picker-pallete__layer--shadowed`,style:{backgroundImage:"linear-gradient(180deg, rgba(0, 0, 0, 0%), rgba(0, 0, 0, 100%))"}}),this.rgba&&i("div",{class:`${e}-color-picker-handle`,style:{width:Ce,height:Ce,borderRadius:Ue,left:`calc(${this.displayedSv[0]}% - ${Ue})`,bottom:`calc(${this.displayedSv[1]}% - ${Ue})`}},i("div",{class:`${e}-color-picker-handle__fill`,style:{backgroundColor:this.handleColor,borderRadius:Ue,width:Ce,height:Ce}})))}}),Sr=$([m("color-picker-panel",`
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
 `,[yt(),m("input",`
 text-align: center;
 `)]),m("color-picker-checkboard",`
 background: white; 
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 `,[$("&::after",`
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
 `),$("&::after",`
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
 `,[Ee("shadowed",`
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
 `,[Ee("disabled","cursor: not-allowed"),U("value",`
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
 `,[$("&::after",`
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
 `),$("&:focus",`
 outline: none;
 `,[U("fill",[$("&::after",`
 position: absolute;
 top: 0;
 right: 0;
 bottom: 0;
 left: 0;
 background: inherit;
 filter: blur(2px);
 content: "";
 `)])])])])]),Cr=Object.assign(Object.assign({},We.props),{value:String,show:{type:Boolean,default:void 0},defaultShow:Boolean,defaultValue:String,modes:{type:Array,default:()=>["rgb","hex","hsl"]},placement:{type:String,default:"bottom-start"},to:He.propTo,showAlpha:{type:Boolean,default:!0},showPreview:Boolean,swatches:Array,disabled:{type:Boolean,default:void 0},actions:{type:Array,default:null},internalActions:Array,size:String,renderLabel:Function,onComplete:Function,onConfirm:Function,onClear:Function,"onUpdate:show":[Function,Array],onUpdateShow:[Function,Array],"onUpdate:value":[Function,Array],onUpdateValue:[Function,Array]}),Ur=O({name:"ColorPicker",props:Cr,slots:Object,setup(e,{slots:t}){let a=null;function n(r){a=r}let l=null;const{mergedClsPrefixRef:o,namespaceRef:u,inlineThemeDisabled:d,mergedComponentPropsRef:c}=Ge(e),A=Ct(e,{mergedSize:r=>{var h,g;const{size:x}=e;if(x)return x;const{mergedSize:y}=r||{};if(y!=null&&y.value)return y.value;const v=(g=(h=c==null?void 0:c.value)===null||h===void 0?void 0:h.ColorPicker)===null||g===void 0?void 0:g.size;return v||"medium"}}),{mergedSizeRef:H,mergedDisabledRef:P}=A,{localeRef:D}=qt("global"),T=We("ColorPicker","-color-picker",Sr,Qt,e,o);zt(qe,{themeRef:T,renderLabelRef:Ve(e,"renderLabel"),colorPickerSlots:t});const j=C(e.defaultShow),me=Oe(Ve(e,"show"),j);function le(r){const{onUpdateShow:h,"onUpdate:show":g}=e;h&&_e(h,r),g&&_e(g,r),j.value=r}const{defaultValue:ie}=e,p=C(ie===void 0?er(e.modes,e.showAlpha):ie),s=Oe(Ve(e,"value"),p),w=C([s.value]),f=C(0),q=V(()=>ye(s.value)),{modes:se}=e,M=C(ye(s.value)||se[0]||"rgb");function X(){const{modes:r}=e,{value:h}=M,g=r.findIndex(x=>x===h);~g?M.value=r[(g+1)%r.length]:M.value="rgb"}let R,I,W,J,E,N,b,F;const ge=V(()=>{const{value:r}=s;if(!r)return null;switch(q.value){case"hsv":return oe(r);case"hsl":return[R,I,W,F]=pe(r),[...Ke(R,I,W),F];case"rgb":case"hex":return[E,N,b,F]=L(r),[...De(E,N,b),F]}}),ee=V(()=>{const{value:r}=s;if(!r)return null;switch(q.value){case"rgb":case"hex":return L(r);case"hsv":return[R,I,J,F]=oe(r),[...Y(R,I,J),F];case"hsl":return[R,I,W,F]=pe(r),[...Pe(R,I,W),F]}}),Ae=V(()=>{const{value:r}=s;if(!r)return null;switch(q.value){case"hsl":return pe(r);case"hsv":return[R,I,J,F]=oe(r),[...$e(R,I,J),F];case"rgb":case"hex":return[E,N,b,F]=L(r),[...Me(E,N,b),F]}}),Qe=V(()=>{switch(M.value){case"rgb":case"hex":return ee.value;case"hsv":return ge.value;case"hsl":return Ae.value}}),ke=C(0),Re=C(1),ze=C([0,0]);function et(r,h){const{value:g}=ge,x=ke.value,y=g?g[3]:1;ze.value=[r,h];const{showAlpha:v}=e;switch(M.value){case"hsv":_((v?ae:Fe)([x,r,h,y]),"cursor");break;case"hsl":_((v?K:Be)([...$e(x,r,h),y]),"cursor");break;case"rgb":_((v?Z:Te)([...Y(x,r,h),y]),"cursor");break;case"hex":_((v?Q:xe)([...Y(x,r,h),y]),"cursor");break}}function tt(r){ke.value=r;const{value:h}=ge;if(!h)return;const[,g,x,y]=h,{showAlpha:v}=e;switch(M.value){case"hsv":_((v?ae:Fe)([r,g,x,y]),"cursor");break;case"rgb":_((v?Z:Te)([...Y(r,g,x),y]),"cursor");break;case"hex":_((v?Q:xe)([...Y(r,g,x),y]),"cursor");break;case"hsl":_((v?K:Be)([...$e(r,g,x),y]),"cursor");break}}function rt(r){switch(M.value){case"hsv":[R,I,J]=ge.value,_(ae([R,I,J,r]),"cursor");break;case"rgb":[E,N,b]=ee.value,_(Z([E,N,b,r]),"cursor");break;case"hex":[E,N,b]=ee.value,_(Q([E,N,b,r]),"cursor");break;case"hsl":[R,I,W]=Ae.value,_(K([R,I,W,r]),"cursor");break}Re.value=r}function _(r,h){h==="cursor"?l=r:l=null;const{nTriggerFormChange:g,nTriggerFormInput:x}=A,{onUpdateValue:y,"onUpdate:value":v}=e;y&&_e(y,r),v&&_e(v,r),g(),x(),p.value=r}function nt(r){_(r,"input"),Rt(ue)}function ue(r=!0){const{value:h}=s;if(h){const{nTriggerFormChange:g,nTriggerFormInput:x}=A,{onComplete:y}=e;y&&y(h);const{value:v}=w,{value:B}=f;r&&(v.splice(B+1,v.length,h),f.value=B+1),g(),x()}}function ot(){const{value:r}=f;r-1<0||(_(w.value[r-1],"input"),ue(!1),f.value=r-1)}function at(){const{value:r}=f;r<0||r+1>=w.value.length||(_(w.value[r+1],"input"),ue(!1),f.value=r+1)}function lt(){_(null,"input");const{onClear:r}=e;r&&r(),le(!1)}function it(){const{value:r}=s,{onConfirm:h}=e;h&&h(r),le(!1)}const st=V(()=>f.value>=1),ut=V(()=>{const{value:r}=w;return r.length>1&&f.value<r.length-1});Ut(me,r=>{r||(w.value=[s.value],f.value=0)}),Xe(()=>{if(!(l&&l===s.value)){const{value:r}=ge;r&&(ke.value=r[0],Re.value=r[3],ze.value=[r[1],r[2]])}l=null});const Ie=V(()=>{const{value:r}=H,{common:{cubicBezierEaseInOut:h},self:{textColor:g,color:x,panelFontSize:y,boxShadow:v,border:B,borderRadius:z,dividerColor:te,[Le("height",r)]:ct,[Le("fontSize",r)]:pt}}=T.value;return{"--n-bezier":h,"--n-text-color":g,"--n-color":x,"--n-panel-font-size":y,"--n-font-size":pt,"--n-box-shadow":v,"--n-border":B,"--n-border-radius":z,"--n-height":ct,"--n-divider-color":te}}),G=d?$t("color-picker",V(()=>H.value[0]),Ie,e):void 0;function dt(){var r;const{value:h}=ee,{value:g}=ke,{internalActions:x,modes:y,actions:v}=e,{value:B}=T,{value:z}=o;return i("div",{class:[`${z}-color-picker-panel`,G==null?void 0:G.themeClass.value],onDragstart:te=>{te.preventDefault()},style:d?void 0:Ie.value},i("div",{class:`${z}-color-picker-control`},i(_r,{clsPrefix:z,rgba:h,displayedHue:g,displayedSv:ze.value,onUpdateSV:et,onComplete:ue}),i("div",{class:`${z}-color-picker-preview`},i("div",{class:`${z}-color-picker-preview__sliders`},i(wr,{clsPrefix:z,hue:g,onUpdateHue:tt,onComplete:ue}),e.showAlpha?i(lr,{clsPrefix:z,rgba:h,alpha:Re.value,onUpdateAlpha:rt,onComplete:ue}):null),e.showPreview?i(br,{clsPrefix:z,mode:M.value,color:ee.value&&xe(ee.value),onUpdateColor:te=>{_(te,"input")}}):null),i(hr,{clsPrefix:z,showAlpha:e.showAlpha,mode:M.value,modes:y,onUpdateMode:X,value:s.value,valueArr:Qe.value,onUpdateValue:nt}),((r=e.swatches)===null||r===void 0?void 0:r.length)&&i(gr,{clsPrefix:z,mode:M.value,swatches:e.swatches,onUpdateColor:te=>{_(te,"input")}})),v!=null&&v.length?i("div",{class:`${z}-color-picker-action`},v.includes("confirm")&&i(be,{size:"small",onClick:it,theme:B.peers.Button,themeOverrides:B.peerOverrides.Button},{default:()=>D.value.confirm}),v.includes("clear")&&i(be,{size:"small",onClick:lt,disabled:!s.value,theme:B.peers.Button,themeOverrides:B.peerOverrides.Button},{default:()=>D.value.clear})):null,t.action?i("div",{class:`${z}-color-picker-action`},{default:t.action}):x?i("div",{class:`${z}-color-picker-action`},x.includes("undo")&&i(be,{size:"small",onClick:ot,disabled:!st.value,theme:B.peers.Button,themeOverrides:B.peerOverrides.Button},{default:()=>D.value.undo}),x.includes("redo")&&i(be,{size:"small",onClick:at,disabled:!ut.value,theme:B.peers.Button,themeOverrides:B.peerOverrides.Button},{default:()=>D.value.redo})):null)}return{mergedClsPrefix:o,namespace:u,hsla:Ae,rgba:ee,mergedShow:me,mergedDisabled:P,isMounted:At(),adjustedTo:He(e),mergedValue:s,handleTriggerClick(){P.value||le(!0)},setTriggerRef:n,handleClickOutside(r){if(a instanceof Element){if(a.contains(Ne(r)))return}else if(a&&a.$el.contains(Ne(r)))return;le(!1)},renderPanel:dt,cssVars:d?void 0:Ie,themeClass:G==null?void 0:G.themeClass,onRender:G==null?void 0:G.onRender}},render(){const{mergedClsPrefix:e,onRender:t}=this;return t==null||t(),i(Ht,null,{default:()=>[i(Tt,null,{default:()=>kt(this.$slots.trigger,{value:this.mergedValue,onClick:this.handleTriggerClick,ref:this.setTriggerRef},a=>a||i(vr,{clsPrefix:e,value:this.mergedValue,hsla:this.hsla,style:this.cssVars,ref:this.setTriggerRef,disabled:this.mergedDisabled,class:this.themeClass,onClick:this.mergedDisabled?void 0:this.handleTriggerClick}))}),i(Ft,{placement:this.placement,show:this.mergedShow,containerClass:this.namespace,teleportDisabled:this.adjustedTo===He.tdkey,to:this.adjustedTo},{default:()=>i(wt,{name:"fade-in-scale-up-transition",appear:this.isMounted},{default:()=>this.mergedShow?_t(this.renderPanel(),[[St,this.handleClickOutside,void 0,{capture:!0}]]):null})})]})}}),Er={__name:"CategoryList",setup(e){const t=Nt(),a=Et(),n=C(null),l=C([]),o=C(!1),u=C(null),d=C(!1),c=C({name:"",type:1,parentId:0,color:"#3498db",icon:""}),A=[{label:"支出",value:1},{label:"收入",value:2}],H=V(()=>{const p=l.value.filter(s=>s.parentId===0&&s.type===c.value.type);return[{label:"顶级分类",value:0},...p.map(s=>({label:s.name,value:s.id}))]}),P=V(()=>{const p=n.value?l.value.filter(s=>s.type===n.value):l.value;return D(p)});function D(p){const s=p.filter(f=>f.parentId===0),w={};return p.forEach(f=>{w[f.id]={key:f.id,label:f.name,...f,children:[]}}),p.forEach(f=>{f.parentId!==0&&w[f.parentId]&&w[f.parentId].children.push(w[f.id])}),s.map(f=>w[f.id])}function T({option:p}){return i("div",{style:{display:"flex",alignItems:"center",gap:"8px"}},[i("span",{style:{width:"12px",height:"12px",borderRadius:"50%",background:p.color||"#ccc",display:"inline-block",flexShrink:0}}),i("span",{},p.name),p.isSystem?i("n-tag",{size:"tiny",type:"info",bordered:!1,style:{marginLeft:"4px"}},{default:()=>"系统"}):null,i("div",{style:{marginLeft:"auto",display:"flex",gap:"4px"}},[i("n-button",{size:"tiny",text:!0,type:"primary",onClick:s=>{s.stopPropagation(),j(p)}},{default:()=>"编辑"}),p.isSystem?null:i("n-button",{size:"tiny",text:!0,type:"error",onClick:s=>{s.stopPropagation(),le(p.id)}},{default:()=>"删除"})].filter(Boolean))])}function j(p){u.value=p.id,c.value={name:p.name,type:p.type,parentId:p.parentId,color:p.color,icon:p.icon||""},o.value=!0}async function me(){if(!c.value.name){a.error("请输入名称");return}d.value=!0;try{u.value?(await we.update(u.value,c.value),a.success("更新成功")):(await we.create(c.value),a.success("创建成功")),o.value=!1,u.value=null,ie()}catch{a.error("操作失败")}finally{d.value=!1}}function le(p){t.warning({title:"确认删除",content:"删除后不可恢复，是否继续？",positiveText:"删除",negativeText:"取消",onPositiveClick:async()=>{try{await we.remove(p),a.success("已删除"),ie()}catch{a.error("删除失败")}}})}async function ie(){try{let w=function(f){f.forEach(q=>{var se;s.push(q),(se=q.children)!=null&&se.length&&w(q.children)})};const p=await we.tree(null),s=[];w(p.data),l.value=s}catch{a.error("加载失败")}}return It(ie),(p,s)=>{const w=Zt,f=Kt,q=be,se=ht,M=Je,X=Ot,R=jt,I=Ur,W=Gt,J=Lt,E=Mt,N=Pt;return Dt(),Vt(N,{title:"分类管理"},{"header-extra":S(()=>[k(f,{value:n.value,"onUpdate:value":s[0]||(s[0]=b=>n.value=b),size:"small",style:{"margin-right":"12px"}},{default:S(()=>[k(w,{value:null},{default:S(()=>[...s[9]||(s[9]=[de("全部",-1)])]),_:1}),k(w,{value:1},{default:S(()=>[...s[10]||(s[10]=[de("支出",-1)])]),_:1}),k(w,{value:2},{default:S(()=>[...s[11]||(s[11]=[de("收入",-1)])]),_:1})]),_:1},8,["value"]),k(q,{type:"primary",onClick:s[1]||(s[1]=b=>o.value=!0)},{default:S(()=>[...s[12]||(s[12]=[de("新增分类",-1)])]),_:1})]),default:S(()=>[k(se,{data:P.value,"block-line":"","show-line":"","render-label":T,style:{"max-height":"70vh","overflow-y":"auto"}},null,8,["data"]),k(E,{show:o.value,"onUpdate:show":s[8]||(s[8]=b=>o.value=b),preset:"card",title:u.value?"编辑分类":"新增分类",style:{width:"450px"}},{default:S(()=>[k(J,{ref:"formRef",model:c.value,"label-placement":"left","label-width":"80"},{default:S(()=>[k(X,{label:"名称",required:""},{default:S(()=>[k(M,{value:c.value.name,"onUpdate:value":s[2]||(s[2]=b=>c.value.name=b),placeholder:"如：餐饮"},null,8,["value"])]),_:1}),k(X,{label:"类型",required:""},{default:S(()=>[k(R,{value:c.value.type,"onUpdate:value":s[3]||(s[3]=b=>c.value.type=b),options:A},null,8,["value"])]),_:1}),k(X,{label:"父分类"},{default:S(()=>[k(R,{value:c.value.parentId,"onUpdate:value":s[4]||(s[4]=b=>c.value.parentId=b),options:H.value,clearable:"",placeholder:"顶级分类"},null,8,["value","options"])]),_:1}),k(X,{label:"颜色"},{default:S(()=>[k(I,{value:c.value.color,"onUpdate:value":s[5]||(s[5]=b=>c.value.color=b),"show-alpha":!1},null,8,["value"])]),_:1}),k(X,{label:"图标"},{default:S(()=>[k(M,{value:c.value.icon,"onUpdate:value":s[6]||(s[6]=b=>c.value.icon=b),placeholder:"图标标识"},null,8,["value"])]),_:1}),k(X,null,{default:S(()=>[k(W,null,{default:S(()=>[k(q,{type:"primary",loading:d.value,onClick:me},{default:S(()=>[...s[13]||(s[13]=[de("保存",-1)])]),_:1},8,["loading"]),k(q,{onClick:s[7]||(s[7]=b=>o.value=!1)},{default:S(()=>[...s[14]||(s[14]=[de("取消",-1)])]),_:1})]),_:1})]),_:1})]),_:1},8,["model"])]),_:1},8,["show","title"])]),_:1})}}};export{Er as default};
