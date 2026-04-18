import{p as Pe,B as Ne,V as Ce,a as Re,r as Ie,N as ke,b as ae}from"./Popover-DIG6BWOD.js";import{r as D,aD as se,d as B,h as l,c as Ke,a as ze,e as Ae,f as te,y as G,q as $,G as ue,az as Oe,A as q,m,bg as ce,p as j,F as Be,aw as Fe,b6 as Te,bh as _e,bi as De,g as R,ax as $e,o as M,w as ie,i as K,n as O,B as He,u as Le,j as pe,l as Me,t as oe,v as z,a9 as _}from"./index-CmO3RiWV.js";import{N as je}from"./Icon-BvbgQQOp.js";import{h as de,c as We}from"./create-BpHkeWfX.js";import{u as Ee}from"./get-DwjtG2nT.js";import{u as Ue}from"./use-keyboard-CDDUx-0V.js";function qe(e,n,d){const i=D(e.value);let r=null;return se(e,t=>{r!==null&&window.clearTimeout(r),t===!0?d&&!d.value?i.value=!0:r=window.setTimeout(()=>{i.value=!0},n):i.value=!1}),i}function Ge(e){return n=>{n?e.value=n.$el:e.value=null}}const Ve=B({name:"ChevronRight",render(){return l("svg",{viewBox:"0 0 16 16",fill:"none",xmlns:"http://www.w3.org/2000/svg"},l("path",{d:"M5.64645 3.14645C5.45118 3.34171 5.45118 3.65829 5.64645 3.85355L9.79289 8L5.64645 12.1464C5.45118 12.3417 5.45118 12.6583 5.64645 12.8536C5.84171 13.0488 6.15829 13.0488 6.35355 12.8536L10.8536 8.35355C11.0488 8.15829 11.0488 7.84171 10.8536 7.64645L6.35355 3.14645C6.15829 2.95118 5.84171 2.95118 5.64645 3.14645Z",fill:"currentColor"}))}}),Xe={padding:"4px 0",optionIconSizeSmall:"14px",optionIconSizeMedium:"16px",optionIconSizeLarge:"16px",optionIconSizeHuge:"18px",optionSuffixWidthSmall:"14px",optionSuffixWidthMedium:"14px",optionSuffixWidthLarge:"16px",optionSuffixWidthHuge:"16px",optionIconSuffixWidthSmall:"32px",optionIconSuffixWidthMedium:"32px",optionIconSuffixWidthLarge:"36px",optionIconSuffixWidthHuge:"36px",optionPrefixWidthSmall:"14px",optionPrefixWidthMedium:"14px",optionPrefixWidthLarge:"16px",optionPrefixWidthHuge:"16px",optionIconPrefixWidthSmall:"36px",optionIconPrefixWidthMedium:"36px",optionIconPrefixWidthLarge:"40px",optionIconPrefixWidthHuge:"40px"};function Ze(e){const{primaryColor:n,textColor2:d,dividerColor:i,hoverColor:r,popoverColor:t,invertedColor:f,borderRadius:v,fontSizeSmall:p,fontSizeMedium:g,fontSizeLarge:x,fontSizeHuge:y,heightSmall:N,heightMedium:S,heightLarge:P,heightHuge:I,textColor3:w,opacityDisabled:C}=e;return Object.assign(Object.assign({},Xe),{optionHeightSmall:N,optionHeightMedium:S,optionHeightLarge:P,optionHeightHuge:I,borderRadius:v,fontSizeSmall:p,fontSizeMedium:g,fontSizeLarge:x,fontSizeHuge:y,optionTextColor:d,optionTextColorHover:d,optionTextColorActive:n,optionTextColorChildActive:n,color:t,dividerColor:i,suffixColor:d,prefixColor:d,optionColorHover:r,optionColorActive:Ae(n,{alpha:.1}),groupHeaderTextColor:w,optionTextColorInverted:"#BBB",optionTextColorHoverInverted:"#FFF",optionTextColorActiveInverted:"#FFF",optionTextColorChildActiveInverted:"#FFF",colorInverted:f,dividerColorInverted:"#BBB",suffixColorInverted:"#BBB",prefixColorInverted:"#BBB",optionColorHoverInverted:n,optionColorActiveInverted:n,groupHeaderTextColorInverted:"#AAA",optionOpacityDisabled:C})}const Je=Ke({name:"Dropdown",common:ze,peers:{Popover:Pe},self:Ze}),re=te("n-dropdown-menu"),V=te("n-dropdown"),le=te("n-dropdown-option"),fe=B({name:"DropdownDivider",props:{clsPrefix:{type:String,required:!0}},render(){return l("div",{class:`${this.clsPrefix}-dropdown-divider`})}}),Qe=B({name:"DropdownGroupHeader",props:{clsPrefix:{type:String,required:!0},tmNode:{type:Object,required:!0}},setup(){const{showIconRef:e,hasSubmenuRef:n}=$(re),{renderLabelRef:d,labelFieldRef:i,nodePropsRef:r,renderOptionRef:t}=$(V);return{labelField:i,showIcon:e,hasSubmenu:n,renderLabel:d,nodeProps:r,renderOption:t}},render(){var e;const{clsPrefix:n,hasSubmenu:d,showIcon:i,nodeProps:r,renderLabel:t,renderOption:f}=this,{rawNode:v}=this.tmNode,p=l("div",Object.assign({class:`${n}-dropdown-option`},r==null?void 0:r(v)),l("div",{class:`${n}-dropdown-option-body ${n}-dropdown-option-body--group`},l("div",{"data-dropdown-option":!0,class:[`${n}-dropdown-option-body__prefix`,i&&`${n}-dropdown-option-body__prefix--show-icon`]},G(v.icon)),l("div",{class:`${n}-dropdown-option-body__label`,"data-dropdown-option":!0},t?t(v):G((e=v.title)!==null&&e!==void 0?e:v[this.labelField])),l("div",{class:[`${n}-dropdown-option-body__suffix`,d&&`${n}-dropdown-option-body__suffix--has-submenu`],"data-dropdown-option":!0})));return f?f({node:p,option:v}):p}});function ne(e,n){return e.type==="submenu"||e.type===void 0&&e[n]!==void 0}function Ye(e){return e.type==="group"}function ve(e){return e.type==="divider"}function eo(e){return e.type==="render"}const he=B({name:"DropdownOption",props:{clsPrefix:{type:String,required:!0},tmNode:{type:Object,required:!0},parentKey:{type:[String,Number],default:null},placement:{type:String,default:"right-start"},props:Object,scrollable:Boolean},setup(e){const n=$(V),{hoverKeyRef:d,keyboardKeyRef:i,lastToggledSubmenuKeyRef:r,pendingKeyPathRef:t,activeKeyPathRef:f,animatedRef:v,mergedShowRef:p,renderLabelRef:g,renderIconRef:x,labelFieldRef:y,childrenFieldRef:N,renderOptionRef:S,nodePropsRef:P,menuPropsRef:I}=n,w=$(le,null),C=$(re),A=$(ce),E=m(()=>e.tmNode.rawNode),W=m(()=>{const{value:o}=N;return ne(e.tmNode.rawNode,o)}),X=m(()=>{const{disabled:o}=e.tmNode;return o}),Z=m(()=>{if(!W.value)return!1;const{key:o,disabled:u}=e.tmNode;if(u)return!1;const{value:b}=d,{value:F}=i,{value:ee}=r,{value:T}=t;return b!==null?T.includes(o):F!==null?T.includes(o)&&T[T.length-1]!==o:ee!==null?T.includes(o):!1}),J=m(()=>i.value===null&&!v.value),Q=qe(Z,300,J),Y=m(()=>!!(w!=null&&w.enteringSubmenuRef.value)),H=D(!1);j(le,{enteringSubmenuRef:H});function L(){H.value=!0}function U(){H.value=!1}function k(){const{parentKey:o,tmNode:u}=e;u.disabled||p.value&&(r.value=o,i.value=null,d.value=u.key)}function a(){const{tmNode:o}=e;o.disabled||p.value&&d.value!==o.key&&k()}function s(o){if(e.tmNode.disabled||!p.value)return;const{relatedTarget:u}=o;u&&!de({target:u},"dropdownOption")&&!de({target:u},"scrollbarRail")&&(d.value=null)}function c(){const{value:o}=W,{tmNode:u}=e;p.value&&!o&&!u.disabled&&(n.doSelect(u.key,u.rawNode),n.doUpdateShow(!1))}return{labelField:y,renderLabel:g,renderIcon:x,siblingHasIcon:C.showIconRef,siblingHasSubmenu:C.hasSubmenuRef,menuProps:I,popoverBody:A,animated:v,mergedShowSubmenu:m(()=>Q.value&&!Y.value),rawNode:E,hasSubmenu:W,pending:q(()=>{const{value:o}=t,{key:u}=e.tmNode;return o.includes(u)}),childActive:q(()=>{const{value:o}=f,{key:u}=e.tmNode,b=o.findIndex(F=>u===F);return b===-1?!1:b<o.length-1}),active:q(()=>{const{value:o}=f,{key:u}=e.tmNode,b=o.findIndex(F=>u===F);return b===-1?!1:b===o.length-1}),mergedDisabled:X,renderOption:S,nodeProps:P,handleClick:c,handleMouseMove:a,handleMouseEnter:k,handleMouseLeave:s,handleSubmenuBeforeEnter:L,handleSubmenuAfterEnter:U}},render(){var e,n;const{animated:d,rawNode:i,mergedShowSubmenu:r,clsPrefix:t,siblingHasIcon:f,siblingHasSubmenu:v,renderLabel:p,renderIcon:g,renderOption:x,nodeProps:y,props:N,scrollable:S}=this;let P=null;if(r){const A=(e=this.menuProps)===null||e===void 0?void 0:e.call(this,i,i.children);P=l(me,Object.assign({},A,{clsPrefix:t,scrollable:this.scrollable,tmNodes:this.tmNode.children,parentKey:this.tmNode.key}))}const I={class:[`${t}-dropdown-option-body`,this.pending&&`${t}-dropdown-option-body--pending`,this.active&&`${t}-dropdown-option-body--active`,this.childActive&&`${t}-dropdown-option-body--child-active`,this.mergedDisabled&&`${t}-dropdown-option-body--disabled`],onMousemove:this.handleMouseMove,onMouseenter:this.handleMouseEnter,onMouseleave:this.handleMouseLeave,onClick:this.handleClick},w=y==null?void 0:y(i),C=l("div",Object.assign({class:[`${t}-dropdown-option`,w==null?void 0:w.class],"data-dropdown-option":!0},w),l("div",ue(I,N),[l("div",{class:[`${t}-dropdown-option-body__prefix`,f&&`${t}-dropdown-option-body__prefix--show-icon`]},[g?g(i):G(i.icon)]),l("div",{"data-dropdown-option":!0,class:`${t}-dropdown-option-body__label`},p?p(i):G((n=i[this.labelField])!==null&&n!==void 0?n:i.title)),l("div",{"data-dropdown-option":!0,class:[`${t}-dropdown-option-body__suffix`,v&&`${t}-dropdown-option-body__suffix--has-submenu`]},this.hasSubmenu?l(je,null,{default:()=>l(Ve,null)}):null)]),this.hasSubmenu?l(Ne,null,{default:()=>[l(Ce,null,{default:()=>l("div",{class:`${t}-dropdown-offset-container`},l(Re,{show:this.mergedShowSubmenu,placement:this.placement,to:S&&this.popoverBody||void 0,teleportDisabled:!S},{default:()=>l("div",{class:`${t}-dropdown-menu-wrapper`},d?l(Oe,{onBeforeEnter:this.handleSubmenuBeforeEnter,onAfterEnter:this.handleSubmenuAfterEnter,name:"fade-in-scale-up-transition",appear:!0},{default:()=>P}):P)}))})]}):null);return x?x({node:C,option:i}):C}}),oo=B({name:"NDropdownGroup",props:{clsPrefix:{type:String,required:!0},tmNode:{type:Object,required:!0},parentKey:{type:[String,Number],default:null}},render(){const{tmNode:e,parentKey:n,clsPrefix:d}=this,{children:i}=e;return l(Be,null,l(Qe,{clsPrefix:d,tmNode:e,key:e.key}),i==null?void 0:i.map(r=>{const{rawNode:t}=r;return t.show===!1?null:ve(t)?l(fe,{clsPrefix:d,key:r.key}):r.isGroup?(Fe("dropdown","`group` node is not allowed to be put in `group` node."),null):l(he,{clsPrefix:d,tmNode:r,parentKey:n,key:r.key})}))}}),no=B({name:"DropdownRenderOption",props:{tmNode:{type:Object,required:!0}},render(){const{rawNode:{render:e,props:n}}=this.tmNode;return l("div",n,[e==null?void 0:e()])}}),me=B({name:"DropdownMenu",props:{scrollable:Boolean,showArrow:Boolean,arrowStyle:[String,Object],clsPrefix:{type:String,required:!0},tmNodes:{type:Array,default:()=>[]},parentKey:{type:[String,Number],default:null}},setup(e){const{renderIconRef:n,childrenFieldRef:d}=$(V);j(re,{showIconRef:m(()=>{const r=n.value;return e.tmNodes.some(t=>{var f;if(t.isGroup)return(f=t.children)===null||f===void 0?void 0:f.some(({rawNode:p})=>r?r(p):p.icon);const{rawNode:v}=t;return r?r(v):v.icon})}),hasSubmenuRef:m(()=>{const{value:r}=d;return e.tmNodes.some(t=>{var f;if(t.isGroup)return(f=t.children)===null||f===void 0?void 0:f.some(({rawNode:p})=>ne(p,r));const{rawNode:v}=t;return ne(v,r)})})});const i=D(null);return j(_e,null),j(De,null),j(ce,i),{bodyRef:i}},render(){const{parentKey:e,clsPrefix:n,scrollable:d}=this,i=this.tmNodes.map(r=>{const{rawNode:t}=r;return t.show===!1?null:eo(t)?l(no,{tmNode:r,key:r.key}):ve(t)?l(fe,{clsPrefix:n,key:r.key}):Ye(t)?l(oo,{clsPrefix:n,tmNode:r,parentKey:e,key:r.key}):l(he,{clsPrefix:n,tmNode:r,parentKey:e,key:r.key,props:t.props,scrollable:d})});return l("div",{class:[`${n}-dropdown-menu`,d&&`${n}-dropdown-menu--scrollable`],ref:"bodyRef"},d?l(Te,{contentClass:`${n}-dropdown-menu__content`},{default:()=>i}):i,this.showArrow?Ie({clsPrefix:n,arrowStyle:this.arrowStyle,arrowClass:void 0,arrowWrapperClass:void 0,arrowWrapperStyle:void 0}):null)}}),to=R("dropdown-menu",`
 transform-origin: var(--v-transform-origin);
 background-color: var(--n-color);
 border-radius: var(--n-border-radius);
 box-shadow: var(--n-box-shadow);
 position: relative;
 transition:
 background-color .3s var(--n-bezier),
 box-shadow .3s var(--n-bezier);
`,[$e(),R("dropdown-option",`
 position: relative;
 `,[M("a",`
 text-decoration: none;
 color: inherit;
 outline: none;
 `,[M("&::before",`
 content: "";
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 `)]),R("dropdown-option-body",`
 display: flex;
 cursor: pointer;
 position: relative;
 height: var(--n-option-height);
 line-height: var(--n-option-height);
 font-size: var(--n-font-size);
 color: var(--n-option-text-color);
 transition: color .3s var(--n-bezier);
 `,[M("&::before",`
 content: "";
 position: absolute;
 top: 0;
 bottom: 0;
 left: 4px;
 right: 4px;
 transition: background-color .3s var(--n-bezier);
 border-radius: var(--n-border-radius);
 `),ie("disabled",[K("pending",`
 color: var(--n-option-text-color-hover);
 `,[O("prefix, suffix",`
 color: var(--n-option-text-color-hover);
 `),M("&::before","background-color: var(--n-option-color-hover);")]),K("active",`
 color: var(--n-option-text-color-active);
 `,[O("prefix, suffix",`
 color: var(--n-option-text-color-active);
 `),M("&::before","background-color: var(--n-option-color-active);")]),K("child-active",`
 color: var(--n-option-text-color-child-active);
 `,[O("prefix, suffix",`
 color: var(--n-option-text-color-child-active);
 `)])]),K("disabled",`
 cursor: not-allowed;
 opacity: var(--n-option-opacity-disabled);
 `),K("group",`
 font-size: calc(var(--n-font-size) - 1px);
 color: var(--n-group-header-text-color);
 `,[O("prefix",`
 width: calc(var(--n-option-prefix-width) / 2);
 `,[K("show-icon",`
 width: calc(var(--n-option-icon-prefix-width) / 2);
 `)])]),O("prefix",`
 width: var(--n-option-prefix-width);
 display: flex;
 justify-content: center;
 align-items: center;
 color: var(--n-prefix-color);
 transition: color .3s var(--n-bezier);
 z-index: 1;
 `,[K("show-icon",`
 width: var(--n-option-icon-prefix-width);
 `),R("icon",`
 font-size: var(--n-option-icon-size);
 `)]),O("label",`
 white-space: nowrap;
 flex: 1;
 z-index: 1;
 `),O("suffix",`
 box-sizing: border-box;
 flex-grow: 0;
 flex-shrink: 0;
 display: flex;
 justify-content: flex-end;
 align-items: center;
 min-width: var(--n-option-suffix-width);
 padding: 0 8px;
 transition: color .3s var(--n-bezier);
 color: var(--n-suffix-color);
 z-index: 1;
 `,[K("has-submenu",`
 width: var(--n-option-icon-suffix-width);
 `),R("icon",`
 font-size: var(--n-option-icon-size);
 `)]),R("dropdown-menu","pointer-events: all;")]),R("dropdown-offset-container",`
 pointer-events: none;
 position: absolute;
 left: 0;
 right: 0;
 top: -4px;
 bottom: -4px;
 `)]),R("dropdown-divider",`
 transition: background-color .3s var(--n-bezier);
 background-color: var(--n-divider-color);
 height: 1px;
 margin: 4px 0;
 `),R("dropdown-menu-wrapper",`
 transform-origin: var(--v-transform-origin);
 width: fit-content;
 `),M(">",[R("scrollbar",`
 height: inherit;
 max-height: inherit;
 `)]),ie("scrollable",`
 padding: var(--n-padding);
 `),K("scrollable",[O("content",`
 padding: var(--n-padding);
 `)])]),ro={animated:{type:Boolean,default:!0},keyboard:{type:Boolean,default:!0},size:String,inverted:Boolean,placement:{type:String,default:"bottom"},onSelect:[Function,Array],options:{type:Array,default:()=>[]},menuProps:Function,showArrow:Boolean,renderLabel:Function,renderIcon:Function,renderOption:Function,nodeProps:Function,labelField:{type:String,default:"label"},keyField:{type:String,default:"key"},childrenField:{type:String,default:"children"},value:[String,Number]},io=Object.keys(ae),lo=Object.assign(Object.assign(Object.assign({},ae),ro),pe.props),vo=B({name:"Dropdown",inheritAttrs:!1,props:lo,setup(e){const n=D(!1),d=Ee(z(e,"show"),n),i=m(()=>{const{keyField:a,childrenField:s}=e;return We(e.options,{getKey(c){return c[a]},getDisabled(c){return c.disabled===!0},getIgnored(c){return c.type==="divider"||c.type==="render"},getChildren(c){return c[s]}})}),r=m(()=>i.value.treeNodes),t=D(null),f=D(null),v=D(null),p=m(()=>{var a,s,c;return(c=(s=(a=t.value)!==null&&a!==void 0?a:f.value)!==null&&s!==void 0?s:v.value)!==null&&c!==void 0?c:null}),g=m(()=>i.value.getPath(p.value).keyPath),x=m(()=>i.value.getPath(e.value).keyPath),y=q(()=>e.keyboard&&d.value);Ue({keydown:{ArrowUp:{prevent:!0,handler:J},ArrowRight:{prevent:!0,handler:Z},ArrowDown:{prevent:!0,handler:Q},ArrowLeft:{prevent:!0,handler:X},Enter:{prevent:!0,handler:Y},Escape:W}},y);const{mergedClsPrefixRef:N,inlineThemeDisabled:S,mergedComponentPropsRef:P}=Le(e),I=m(()=>{var a,s;return e.size||((s=(a=P==null?void 0:P.value)===null||a===void 0?void 0:a.Dropdown)===null||s===void 0?void 0:s.size)||"medium"}),w=pe("Dropdown","-dropdown",to,Je,e,N);j(V,{labelFieldRef:z(e,"labelField"),childrenFieldRef:z(e,"childrenField"),renderLabelRef:z(e,"renderLabel"),renderIconRef:z(e,"renderIcon"),hoverKeyRef:t,keyboardKeyRef:f,lastToggledSubmenuKeyRef:v,pendingKeyPathRef:g,activeKeyPathRef:x,animatedRef:z(e,"animated"),mergedShowRef:d,nodePropsRef:z(e,"nodeProps"),renderOptionRef:z(e,"renderOption"),menuPropsRef:z(e,"menuProps"),doSelect:C,doUpdateShow:A}),se(d,a=>{!e.animated&&!a&&E()});function C(a,s){const{onSelect:c}=e;c&&oe(c,a,s)}function A(a){const{"onUpdate:show":s,onUpdateShow:c}=e;s&&oe(s,a),c&&oe(c,a),n.value=a}function E(){t.value=null,f.value=null,v.value=null}function W(){A(!1)}function X(){L("left")}function Z(){L("right")}function J(){L("up")}function Q(){L("down")}function Y(){const a=H();a!=null&&a.isLeaf&&d.value&&(C(a.key,a.rawNode),A(!1))}function H(){var a;const{value:s}=i,{value:c}=p;return!s||c===null?null:(a=s.getNode(c))!==null&&a!==void 0?a:null}function L(a){const{value:s}=p,{value:{getFirstAvailableNode:c}}=i;let o=null;if(s===null){const u=c();u!==null&&(o=u.key)}else{const u=H();if(u){let b;switch(a){case"down":b=u.getNext();break;case"up":b=u.getPrev();break;case"right":b=u.getChild();break;case"left":b=u.getParent();break}b&&(o=b.key)}}o!==null&&(t.value=null,f.value=o)}const U=m(()=>{const{inverted:a}=e,s=I.value,{common:{cubicBezierEaseInOut:c},self:o}=w.value,{padding:u,dividerColor:b,borderRadius:F,optionOpacityDisabled:ee,[_("optionIconSuffixWidth",s)]:T,[_("optionSuffixWidth",s)]:be,[_("optionIconPrefixWidth",s)]:we,[_("optionPrefixWidth",s)]:ge,[_("fontSize",s)]:xe,[_("optionHeight",s)]:ye,[_("optionIconSize",s)]:Se}=o,h={"--n-bezier":c,"--n-font-size":xe,"--n-padding":u,"--n-border-radius":F,"--n-option-height":ye,"--n-option-prefix-width":ge,"--n-option-icon-prefix-width":we,"--n-option-suffix-width":be,"--n-option-icon-suffix-width":T,"--n-option-icon-size":Se,"--n-divider-color":b,"--n-option-opacity-disabled":ee};return a?(h["--n-color"]=o.colorInverted,h["--n-option-color-hover"]=o.optionColorHoverInverted,h["--n-option-color-active"]=o.optionColorActiveInverted,h["--n-option-text-color"]=o.optionTextColorInverted,h["--n-option-text-color-hover"]=o.optionTextColorHoverInverted,h["--n-option-text-color-active"]=o.optionTextColorActiveInverted,h["--n-option-text-color-child-active"]=o.optionTextColorChildActiveInverted,h["--n-prefix-color"]=o.prefixColorInverted,h["--n-suffix-color"]=o.suffixColorInverted,h["--n-group-header-text-color"]=o.groupHeaderTextColorInverted):(h["--n-color"]=o.color,h["--n-option-color-hover"]=o.optionColorHover,h["--n-option-color-active"]=o.optionColorActive,h["--n-option-text-color"]=o.optionTextColor,h["--n-option-text-color-hover"]=o.optionTextColorHover,h["--n-option-text-color-active"]=o.optionTextColorActive,h["--n-option-text-color-child-active"]=o.optionTextColorChildActive,h["--n-prefix-color"]=o.prefixColor,h["--n-suffix-color"]=o.suffixColor,h["--n-group-header-text-color"]=o.groupHeaderTextColor),h}),k=S?Me("dropdown",m(()=>`${I.value[0]}${e.inverted?"i":""}`),U,e):void 0;return{mergedClsPrefix:N,mergedTheme:w,mergedSize:I,tmNodes:r,mergedShow:d,handleAfterLeave:()=>{e.animated&&E()},doUpdateShow:A,cssVars:S?void 0:U,themeClass:k==null?void 0:k.themeClass,onRender:k==null?void 0:k.onRender}},render(){const e=(i,r,t,f,v)=>{var p;const{mergedClsPrefix:g,menuProps:x}=this;(p=this.onRender)===null||p===void 0||p.call(this);const y=(x==null?void 0:x(void 0,this.tmNodes.map(S=>S.rawNode)))||{},N={ref:Ge(r),class:[i,`${g}-dropdown`,`${g}-dropdown--${this.mergedSize}-size`,this.themeClass],clsPrefix:g,tmNodes:this.tmNodes,style:[...t,this.cssVars],showArrow:this.showArrow,arrowStyle:this.arrowStyle,scrollable:this.scrollable,onMouseenter:f,onMouseleave:v};return l(me,ue(this.$attrs,N,y))},{mergedTheme:n}=this,d={show:this.mergedShow,theme:n.peers.Popover,themeOverrides:n.peerOverrides.Popover,internalOnAfterLeave:this.handleAfterLeave,internalRenderBody:e,onUpdateShow:this.doUpdateShow,"onUpdate:show":void 0};return l(ke,Object.assign({},He(this.$props,io),d),{trigger:()=>{var i,r;return(r=(i=this.$slots).default)===null||r===void 0?void 0:r.call(i)}})}});export{Ve as C,vo as N,Ge as c,Je as d};
