import{g as c,i as n,n as o,o as t,w as I,d as O,h as l,a9 as H,j as v,u as T,an as V,l as L,m as h,ap as u}from"./index-BOHWmnqk.js";import{s as M,a as N,r as W}from"./RadioGroup-A0x3myv2.js";const K=c("radio",`
 line-height: var(--n-label-line-height);
 outline: none;
 position: relative;
 user-select: none;
 -webkit-user-select: none;
 display: inline-flex;
 align-items: flex-start;
 flex-wrap: nowrap;
 font-size: var(--n-font-size);
 word-break: break-word;
`,[n("checked",[o("dot",`
 background-color: var(--n-color-active);
 `)]),o("dot-wrapper",`
 position: relative;
 flex-shrink: 0;
 flex-grow: 0;
 width: var(--n-radio-size);
 `),c("radio-input",`
 position: absolute;
 border: 0;
 width: 0;
 height: 0;
 opacity: 0;
 margin: 0;
 `),o("dot",`
 position: absolute;
 top: 50%;
 left: 0;
 transform: translateY(-50%);
 height: var(--n-radio-size);
 width: var(--n-radio-size);
 background: var(--n-color);
 box-shadow: var(--n-box-shadow);
 border-radius: 50%;
 transition:
 background-color .3s var(--n-bezier),
 box-shadow .3s var(--n-bezier);
 `,[t("&::before",`
 content: "";
 opacity: 0;
 position: absolute;
 left: 4px;
 top: 4px;
 height: calc(100% - 8px);
 width: calc(100% - 8px);
 border-radius: 50%;
 transform: scale(.8);
 background: var(--n-dot-color-active);
 transition: 
 opacity .3s var(--n-bezier),
 background-color .3s var(--n-bezier),
 transform .3s var(--n-bezier);
 `),n("checked",{boxShadow:"var(--n-box-shadow-active)"},[t("&::before",`
 opacity: 1;
 transform: scale(1);
 `)])]),o("label",`
 color: var(--n-text-color);
 padding: var(--n-label-padding);
 font-weight: var(--n-label-font-weight);
 display: inline-block;
 transition: color .3s var(--n-bezier);
 `),I("disabled",`
 cursor: pointer;
 `,[t("&:hover",[o("dot",{boxShadow:"var(--n-box-shadow-hover)"})]),n("focus",[t("&:not(:active)",[o("dot",{boxShadow:"var(--n-box-shadow-focus)"})])])]),n("disabled",`
 cursor: not-allowed;
 `,[o("dot",{boxShadow:"var(--n-box-shadow-disabled)",backgroundColor:"var(--n-color-disabled)"},[t("&::before",{backgroundColor:"var(--n-dot-color-disabled)"}),n("checked",`
 opacity: 1;
 `)]),o("label",{color:"var(--n-text-color-disabled)"}),c("radio-input",`
 cursor: not-allowed;
 `)])]),Y=Object.assign(Object.assign({},v.props),W),J=O({name:"Radio",props:Y,setup(a){const e=M(a),i=v("Radio","-radio",K,N,a,e.mergedClsPrefix),s=h(()=>{const{mergedSize:{value:b}}=e,{common:{cubicBezierEaseInOut:x},self:{boxShadow:w,boxShadowActive:m,boxShadowDisabled:z,boxShadowFocus:k,boxShadowHover:C,color:S,colorDisabled:R,colorActive:_,textColor:y,textColorDisabled:$,dotColorActive:D,dotColorDisabled:P,labelPadding:B,labelLineHeight:E,labelFontWeight:j,[u("fontSize",b)]:A,[u("radioSize",b)]:F}}=i.value;return{"--n-bezier":x,"--n-label-line-height":E,"--n-label-font-weight":j,"--n-box-shadow":w,"--n-box-shadow-active":m,"--n-box-shadow-disabled":z,"--n-box-shadow-focus":k,"--n-box-shadow-hover":C,"--n-color":S,"--n-color-active":_,"--n-color-disabled":R,"--n-dot-color-active":D,"--n-dot-color-disabled":P,"--n-font-size":A,"--n-radio-size":F,"--n-text-color":y,"--n-text-color-disabled":$,"--n-label-padding":B}}),{inlineThemeDisabled:d,mergedClsPrefixRef:f,mergedRtlRef:p}=T(a),g=V("Radio",p,f),r=d?L("radio",h(()=>e.mergedSize.value[0]),s,a):void 0;return Object.assign(e,{rtlEnabled:g,cssVars:d?void 0:s,themeClass:r==null?void 0:r.themeClass,onRender:r==null?void 0:r.onRender})},render(){const{$slots:a,mergedClsPrefix:e,onRender:i,label:s}=this;return i==null||i(),l("label",{class:[`${e}-radio`,this.themeClass,this.rtlEnabled&&`${e}-radio--rtl`,this.mergedDisabled&&`${e}-radio--disabled`,this.renderSafeChecked&&`${e}-radio--checked`,this.focus&&`${e}-radio--focus`],style:this.cssVars},l("div",{class:`${e}-radio__dot-wrapper`}," ",l("div",{class:[`${e}-radio__dot`,this.renderSafeChecked&&`${e}-radio__dot--checked`]}),l("input",{ref:"inputRef",type:"radio",class:`${e}-radio-input`,value:this.value,name:this.mergedName,checked:this.renderSafeChecked,disabled:this.mergedDisabled,onChange:this.handleRadioInputChange,onFocus:this.handleRadioInputFocus,onBlur:this.handleRadioInputBlur})),H(a.default,d=>!d&&!s?null:l("div",{ref:"labelRef",class:`${e}-radio__label`},d||s)))}});export{J as _};
