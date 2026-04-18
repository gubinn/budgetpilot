import{a as oe,e as $,q as te,u as P,aD as E,r as z,A as D,t as B,f as re,v as V,g as I,n as C,i as S,o as T,w as A,d as ne,bn as ae,h as j,j as M,a8 as ie,l as de,m as H,aa as U,p as le}from"./index-Mpan-3ve.js";import{u as G,c as se}from"./get-DiH_SOvm.js";const ue={radioSizeSmall:"14px",radioSizeMedium:"16px",radioSizeLarge:"18px",labelPadding:"0 8px",labelFontWeight:"400"};function ce(e){const{borderColor:o,primaryColor:t,baseColor:i,textColorDisabled:d,inputColorDisabled:h,textColor2:l,opacityDisabled:s,borderRadius:u,fontSizeSmall:v,fontSizeMedium:g,fontSizeLarge:p,heightSmall:c,heightMedium:m,heightLarge:b,lineHeight:x}=e;return Object.assign(Object.assign({},ue),{labelLineHeight:x,buttonHeightSmall:c,buttonHeightMedium:m,buttonHeightLarge:b,fontSizeSmall:v,fontSizeMedium:g,fontSizeLarge:p,boxShadow:`inset 0 0 0 1px ${o}`,boxShadowActive:`inset 0 0 0 1px ${t}`,boxShadowFocus:`inset 0 0 0 1px ${t}, 0 0 0 2px ${$(t,{alpha:.2})}`,boxShadowHover:`inset 0 0 0 1px ${t}`,boxShadowDisabled:`inset 0 0 0 1px ${o}`,color:i,colorDisabled:h,colorActive:"#0000",textColor:l,textColorDisabled:d,dotColorActive:t,dotColorDisabled:o,buttonBorderColor:o,buttonBorderColorActive:t,buttonBorderColorHover:o,buttonColor:i,buttonColorActive:i,buttonTextColor:l,buttonTextColorActive:t,buttonTextColorHover:t,opacityDisabled:s,buttonBoxShadowFocus:`inset 0 0 0 1px ${t}, 0 0 0 2px ${$(t,{alpha:.3})}`,buttonBoxShadowHover:"inset 0 0 0 1px #0000",buttonBoxShadow:"inset 0 0 0 1px #0000",buttonBorderRadius:u})}const be={name:"Radio",common:oe,self:ce},me={name:String,value:{type:[String,Number,Boolean],default:"on"},checked:{type:Boolean,default:void 0},defaultChecked:Boolean,disabled:{type:Boolean,default:void 0},label:String,size:String,onUpdateChecked:[Function,Array],"onUpdate:checked":[Function,Array],checkedValue:{type:Boolean,default:void 0}},L=re("n-radio-group");function xe(e){const o=te(L,null),{mergedClsPrefixRef:t,mergedComponentPropsRef:i}=P(e),d=E(e,{mergedSize(r){var n,a;const{size:f}=e;if(f!==void 0)return f;if(o){const{mergedSizeRef:{value:F}}=o;if(F!==void 0)return F}if(r)return r.mergedSize.value;const _=(a=(n=i==null?void 0:i.value)===null||n===void 0?void 0:n.Radio)===null||a===void 0?void 0:a.size;return _||"medium"},mergedDisabled(r){return!!(e.disabled||o!=null&&o.disabledRef.value||r!=null&&r.disabled.value)}}),{mergedSizeRef:h,mergedDisabledRef:l}=d,s=z(null),u=z(null),v=z(e.defaultChecked),g=V(e,"checked"),p=G(g,v),c=D(()=>o?o.valueRef.value===e.value:p.value),m=D(()=>{const{name:r}=e;if(r!==void 0)return r;if(o)return o.nameRef.value}),b=z(!1);function x(){if(o){const{doUpdateValue:r}=o,{value:n}=e;B(r,n)}else{const{onUpdateChecked:r,"onUpdate:checked":n}=e,{nTriggerFormInput:a,nTriggerFormChange:f}=d;r&&B(r,!0),n&&B(n,!0),a(),f(),v.value=!0}}function k(){l.value||c.value||x()}function y(){k(),s.value&&(s.value.checked=c.value)}function w(){b.value=!1}function R(){b.value=!0}return{mergedClsPrefix:o?o.mergedClsPrefixRef:t,inputRef:s,labelRef:u,mergedName:m,mergedDisabled:l,renderSafeChecked:c,focus:b,mergedSize:h,handleRadioInputChange:y,handleRadioInputBlur:w,handleRadioInputFocus:R}}const he=I("radio-group",`
 display: inline-block;
 font-size: var(--n-font-size);
`,[C("splitor",`
 display: inline-block;
 vertical-align: bottom;
 width: 1px;
 transition:
 background-color .3s var(--n-bezier),
 opacity .3s var(--n-bezier);
 background: var(--n-button-border-color);
 `,[S("checked",{backgroundColor:"var(--n-button-border-color-active)"}),S("disabled",{opacity:"var(--n-opacity-disabled)"})]),S("button-group",`
 white-space: nowrap;
 height: var(--n-height);
 line-height: var(--n-height);
 `,[I("radio-button",{height:"var(--n-height)",lineHeight:"var(--n-height)"}),C("splitor",{height:"var(--n-height)"})]),I("radio-button",`
 vertical-align: bottom;
 outline: none;
 position: relative;
 user-select: none;
 -webkit-user-select: none;
 display: inline-block;
 box-sizing: border-box;
 padding-left: 14px;
 padding-right: 14px;
 white-space: nowrap;
 transition:
 background-color .3s var(--n-bezier),
 opacity .3s var(--n-bezier),
 border-color .3s var(--n-bezier),
 color .3s var(--n-bezier);
 background: var(--n-button-color);
 color: var(--n-button-text-color);
 border-top: 1px solid var(--n-button-border-color);
 border-bottom: 1px solid var(--n-button-border-color);
 `,[I("radio-input",`
 pointer-events: none;
 position: absolute;
 border: 0;
 border-radius: inherit;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 opacity: 0;
 z-index: 1;
 `),C("state-border",`
 z-index: 1;
 pointer-events: none;
 position: absolute;
 box-shadow: var(--n-button-box-shadow);
 transition: box-shadow .3s var(--n-bezier);
 left: -1px;
 bottom: -1px;
 right: -1px;
 top: -1px;
 `),T("&:first-child",`
 border-top-left-radius: var(--n-button-border-radius);
 border-bottom-left-radius: var(--n-button-border-radius);
 border-left: 1px solid var(--n-button-border-color);
 `,[C("state-border",`
 border-top-left-radius: var(--n-button-border-radius);
 border-bottom-left-radius: var(--n-button-border-radius);
 `)]),T("&:last-child",`
 border-top-right-radius: var(--n-button-border-radius);
 border-bottom-right-radius: var(--n-button-border-radius);
 border-right: 1px solid var(--n-button-border-color);
 `,[C("state-border",`
 border-top-right-radius: var(--n-button-border-radius);
 border-bottom-right-radius: var(--n-button-border-radius);
 `)]),A("disabled",`
 cursor: pointer;
 `,[T("&:hover",[C("state-border",`
 transition: box-shadow .3s var(--n-bezier);
 box-shadow: var(--n-button-box-shadow-hover);
 `),A("checked",{color:"var(--n-button-text-color-hover)"})]),S("focus",[T("&:not(:active)",[C("state-border",{boxShadow:"var(--n-button-box-shadow-focus)"})])])]),S("checked",`
 background: var(--n-button-color-active);
 color: var(--n-button-text-color-active);
 border-color: var(--n-button-border-color-active);
 `),S("disabled",`
 cursor: not-allowed;
 opacity: var(--n-opacity-disabled);
 `)])]);function ve(e,o,t){var i;const d=[];let h=!1;for(let l=0;l<e.length;++l){const s=e[l],u=(i=s.type)===null||i===void 0?void 0:i.name;u==="RadioButton"&&(h=!0);const v=s.props;if(u!=="RadioButton"){d.push(s);continue}if(l===0)d.push(s);else{const g=d[d.length-1].props,p=o===g.value,c=g.disabled,m=o===v.value,b=v.disabled,x=(p?2:0)+(c?0:1),k=(m?2:0)+(b?0:1),y={[`${t}-radio-group__splitor--disabled`]:c,[`${t}-radio-group__splitor--checked`]:p},w={[`${t}-radio-group__splitor--disabled`]:b,[`${t}-radio-group__splitor--checked`]:m},R=x<k?w:y;d.push(j("div",{class:[`${t}-radio-group__splitor`,R]}),s)}}return{children:d,isButtonGroup:h}}const fe=Object.assign(Object.assign({},M.props),{name:String,value:[String,Number,Boolean],defaultValue:{type:[String,Number,Boolean],default:null},size:String,disabled:{type:Boolean,default:void 0},"onUpdate:value":[Function,Array],onUpdateValue:[Function,Array]}),Ce=ne({name:"RadioGroup",props:fe,setup(e){const o=z(null),{mergedSizeRef:t,mergedDisabledRef:i,nTriggerFormChange:d,nTriggerFormInput:h,nTriggerFormBlur:l,nTriggerFormFocus:s}=E(e),{mergedClsPrefixRef:u,inlineThemeDisabled:v,mergedRtlRef:g}=P(e),p=M("Radio","-radio-group",he,be,e,u),c=z(e.defaultValue),m=V(e,"value"),b=G(m,c);function x(n){const{onUpdateValue:a,"onUpdate:value":f}=e;a&&B(a,n),f&&B(f,n),c.value=n,d(),h()}function k(n){const{value:a}=o;a&&(a.contains(n.relatedTarget)||s())}function y(n){const{value:a}=o;a&&(a.contains(n.relatedTarget)||l())}le(L,{mergedClsPrefixRef:u,nameRef:V(e,"name"),valueRef:b,disabledRef:i,mergedSizeRef:t,doUpdateValue:x});const w=ie("Radio",g,u),R=H(()=>{const{value:n}=t,{common:{cubicBezierEaseInOut:a},self:{buttonBorderColor:f,buttonBorderColorActive:_,buttonBorderRadius:F,buttonBoxShadow:O,buttonBoxShadowFocus:N,buttonBoxShadowHover:K,buttonColor:q,buttonColorActive:W,buttonTextColor:J,buttonTextColorActive:Q,buttonTextColorHover:X,opacityDisabled:Y,[U("buttonHeight",n)]:Z,[U("fontSize",n)]:ee}}=p.value;return{"--n-font-size":ee,"--n-bezier":a,"--n-button-border-color":f,"--n-button-border-color-active":_,"--n-button-border-radius":F,"--n-button-box-shadow":O,"--n-button-box-shadow-focus":N,"--n-button-box-shadow-hover":K,"--n-button-color":q,"--n-button-color-active":W,"--n-button-text-color":J,"--n-button-text-color-hover":X,"--n-button-text-color-active":Q,"--n-height":Z,"--n-opacity-disabled":Y}}),r=v?de("radio-group",H(()=>t.value[0]),R,e):void 0;return{selfElRef:o,rtlEnabled:w,mergedClsPrefix:u,mergedValue:b,handleFocusout:y,handleFocusin:k,cssVars:v?void 0:R,themeClass:r==null?void 0:r.themeClass,onRender:r==null?void 0:r.onRender}},render(){var e;const{mergedValue:o,mergedClsPrefix:t,handleFocusin:i,handleFocusout:d}=this,{children:h,isButtonGroup:l}=ve(ae(se(this)),o,t);return(e=this.onRender)===null||e===void 0||e.call(this),j("div",{onFocusin:i,onFocusout:d,ref:"selfElRef",class:[`${t}-radio-group`,this.rtlEnabled&&`${t}-radio-group--rtl`,this.themeClass,l&&`${t}-radio-group--button-group`],style:this.cssVars},h)}});export{Ce as _,be as a,me as r,xe as s};
