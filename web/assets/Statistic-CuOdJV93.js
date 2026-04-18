import{a as S,g as r,n as c,d as _,h as l,a9 as u,u as T,j as v,an as R,l as $,m as F}from"./index-f5MniL2I.js";function w(s){const{textColor2:e,textColor3:a,fontSize:o,fontWeight:i}=s;return{labelFontSize:o,labelFontWeight:i,valueFontWeight:i,valueFontSize:"24px",labelTextColor:a,valuePrefixTextColor:e,valueSuffixTextColor:e,valueTextColor:e}}const P={common:S,self:w},W=r("statistic",[c("label",`
 font-weight: var(--n-label-font-weight);
 transition: .3s color var(--n-bezier);
 font-size: var(--n-label-font-size);
 color: var(--n-label-text-color);
 `),r("statistic-value",`
 margin-top: 4px;
 font-weight: var(--n-value-font-weight);
 `,[c("prefix",`
 margin: 0 4px 0 0;
 font-size: var(--n-value-font-size);
 transition: .3s color var(--n-bezier);
 color: var(--n-value-prefix-text-color);
 `,[r("icon",{verticalAlign:"-0.125em"})]),c("content",`
 font-size: var(--n-value-font-size);
 transition: .3s color var(--n-bezier);
 color: var(--n-value-text-color);
 `),c("suffix",`
 margin: 0 0 0 4px;
 font-size: var(--n-value-font-size);
 transition: .3s color var(--n-bezier);
 color: var(--n-value-suffix-text-color);
 `,[r("icon",{verticalAlign:"-0.125em"})])])]),E=Object.assign(Object.assign({},v.props),{tabularNums:Boolean,label:String,value:[String,Number]}),N=_({name:"Statistic",props:E,slots:Object,setup(s){const{mergedClsPrefixRef:e,inlineThemeDisabled:a,mergedRtlRef:o}=T(s),i=v("Statistic","-statistic",W,P,s,e),f=R("Statistic",o,e),t=F(()=>{const{self:{labelFontWeight:x,valueFontSize:b,valueFontWeight:d,valuePrefixTextColor:m,labelTextColor:g,valueSuffixTextColor:h,valueTextColor:p,labelFontSize:z},common:{cubicBezierEaseInOut:C}}=i.value;return{"--n-bezier":C,"--n-label-font-size":z,"--n-label-font-weight":x,"--n-label-text-color":g,"--n-value-font-weight":d,"--n-value-font-size":b,"--n-value-prefix-text-color":m,"--n-value-suffix-text-color":h,"--n-value-text-color":p}}),n=a?$("statistic",void 0,t,s):void 0;return{rtlEnabled:f,mergedClsPrefix:e,cssVars:a?void 0:t,themeClass:n==null?void 0:n.themeClass,onRender:n==null?void 0:n.onRender}},render(){var s;const{mergedClsPrefix:e,$slots:{default:a,label:o,prefix:i,suffix:f}}=this;return(s=this.onRender)===null||s===void 0||s.call(this),l("div",{class:[`${e}-statistic`,this.themeClass,this.rtlEnabled&&`${e}-statistic--rtl`],style:this.cssVars},u(o,t=>l("div",{class:`${e}-statistic__label`},this.label||t)),l("div",{class:`${e}-statistic-value`,style:{fontVariantNumeric:this.tabularNums?"tabular-nums":""}},u(i,t=>t&&l("span",{class:`${e}-statistic-value__prefix`},t)),this.value!==void 0?l("span",{class:`${e}-statistic-value__content`},this.value):u(a,t=>t&&l("span",{class:`${e}-statistic-value__content`},t)),u(f,t=>t&&l("span",{class:`${e}-statistic-value__suffix`},t))))}});export{N as _};
