import{p as Pe,B as Ne,V as Re,a as Ce,r as Ie,N as ke,b as ae}from"./Popover-Ca5JTck5.js";import{r as _,aa as se,c as Ke,a as ze,e as Ae,f as te,d as $,h as a,y as G,q as D,G as ue,al as Oe,A as q,m,bm as ce,p as L,F as Fe,aR as Be,bc as Te,bn as _e,bo as De,g as C,ar as $e,o as j,w as ie,i as K,n as O,B as He,u as Me,j as pe,l as je,t as oe,v as z,ai as T}from"./index-BNEO0cMD.js";import{N as Le}from"./Icon-Cxk0FVpr.js";import{C as We}from"./ChevronRight-D3o8Qp1D.js";import{h as de,c as Ee}from"./create-Crei8FZA.js";import{u as Ue}from"./get-hdHF-9wE.js";import{u as qe}from"./use-keyboard-C-LIJi3N.js";function Ge(e,n,d){const i=_(e.value);let r=null;return se(e,t=>{r!==null&&window.clearTimeout(r),t===!0?d&&!d.value?i.value=!0:r=window.setTimeout(()=>{i.value=!0},n):i.value=!1}),i}function Ve(e){return n=>{n?e.value=n.$el:e.value=null}}const Xe={padding:"4px 0",optionIconSizeSmall:"14px",optionIconSizeMedium:"16px",optionIconSizeLarge:"16px",optionIconSizeHuge:"18px",optionSuffixWidthSmall:"14px",optionSuffixWidthMedium:"14px",optionSuffixWidthLarge:"16px",optionSuffixWidthHuge:"16px",optionIconSuffixWidthSmall:"32px",optionIconSuffixWidthMedium:"32px",optionIconSuffixWidthLarge:"36px",optionIconSuffixWidthHuge:"36px",optionPrefixWidthSmall:"14px",optionPrefixWidthMedium:"14px",optionPrefixWidthLarge:"16px",optionPrefixWidthHuge:"16px",optionIconPrefixWidthSmall:"36px",optionIconPrefixWidthMedium:"36px",optionIconPrefixWidthLarge:"40px",optionIconPrefixWidthHuge:"40px"};function Je(e){const{primaryColor:n,textColor2:d,dividerColor:i,hoverColor:r,popoverColor:t,invertedColor:f,borderRadius:v,fontSizeSmall:p,fontSizeMedium:g,fontSizeLarge:x,fontSizeHuge:y,heightSmall:N,heightMedium:S,heightLarge:P,heightHuge:I,textColor3:w,opacityDisabled:R}=e;return Object.assign(Object.assign({},Xe),{optionHeightSmall:N,optionHeightMedium:S,optionHeightLarge:P,optionHeightHuge:I,borderRadius:v,fontSizeSmall:p,fontSizeMedium:g,fontSizeLarge:x,fontSizeHuge:y,optionTextColor:d,optionTextColorHover:d,optionTextColorActive:n,optionTextColorChildActive:n,color:t,dividerColor:i,suffixColor:d,prefixColor:d,optionColorHover:r,optionColorActive:Ae(n,{alpha:.1}),groupHeaderTextColor:w,optionTextColorInverted:"#BBB",optionTextColorHoverInverted:"#FFF",optionTextColorActiveInverted:"#FFF",optionTextColorChildActiveInverted:"#FFF",colorInverted:f,dividerColorInverted:"#BBB",suffixColorInverted:"#BBB",prefixColorInverted:"#BBB",optionColorHoverInverted:n,optionColorActiveInverted:n,groupHeaderTextColorInverted:"#AAA",optionOpacityDisabled:R})}const Qe=Ke({name:"Dropdown",common:ze,peers:{Popover:Pe},self:Je}),re=te("n-dropdown-menu"),V=te("n-dropdown"),le=te("n-dropdown-option"),fe=$({name:"DropdownDivider",props:{clsPrefix:{type:String,required:!0}},render(){return a("div",{class:`${this.clsPrefix}-dropdown-divider`})}}),Ye=$({name:"DropdownGroupHeader",props:{clsPrefix:{type:String,required:!0},tmNode:{type:Object,required:!0}},setup(){const{showIconRef:e,hasSubmenuRef:n}=D(re),{renderLabelRef:d,labelFieldRef:i,nodePropsRef:r,renderOptionRef:t}=D(V);return{labelField:i,showIcon:e,hasSubmenu:n,renderLabel:d,nodeProps:r,renderOption:t}},render(){var e;const{clsPrefix:n,hasSubmenu:d,showIcon:i,nodeProps:r,renderLabel:t,renderOption:f}=this,{rawNode:v}=this.tmNode,p=a("div",Object.assign({class:`${n}-dropdown-option`},r==null?void 0:r(v)),a("div",{class:`${n}-dropdown-option-body ${n}-dropdown-option-body--group`},a("div",{"data-dropdown-option":!0,class:[`${n}-dropdown-option-body__prefix`,i&&`${n}-dropdown-option-body__prefix--show-icon`]},G(v.icon)),a("div",{class:`${n}-dropdown-option-body__label`,"data-dropdown-option":!0},t?t(v):G((e=v.title)!==null&&e!==void 0?e:v[this.labelField])),a("div",{class:[`${n}-dropdown-option-body__suffix`,d&&`${n}-dropdown-option-body__suffix--has-submenu`],"data-dropdown-option":!0})));return f?f({node:p,option:v}):p}});function ne(e,n){return e.type==="submenu"||e.type===void 0&&e[n]!==void 0}function Ze(e){return e.type==="group"}function ve(e){return e.type==="divider"}function eo(e){return e.type==="render"}const he=$({name:"DropdownOption",props:{clsPrefix:{type:String,required:!0},tmNode:{type:Object,required:!0},parentKey:{type:[String,Number],default:null},placement:{type:String,default:"right-start"},props:Object,scrollable:Boolean},setup(e){const n=D(V),{hoverKeyRef:d,keyboardKeyRef:i,lastToggledSubmenuKeyRef:r,pendingKeyPathRef:t,activeKeyPathRef:f,animatedRef:v,mergedShowRef:p,renderLabelRef:g,renderIconRef:x,labelFieldRef:y,childrenFieldRef:N,renderOptionRef:S,nodePropsRef:P,menuPropsRef:I}=n,w=D(le,null),R=D(re),A=D(ce),E=m(()=>e.tmNode.rawNode),W=m(()=>{const{value:o}=N;return ne(e.tmNode.rawNode,o)}),X=m(()=>{const{disabled:o}=e.tmNode;return o}),J=m(()=>{if(!W.value)return!1;const{key:o,disabled:u}=e.tmNode;if(u)return!1;const{value:b}=d,{value:F}=i,{value:ee}=r,{value:B}=t;return b!==null?B.includes(o):F!==null?B.includes(o)&&B[B.length-1]!==o:ee!==null?B.includes(o):!1}),Q=m(()=>i.value===null&&!v.value),Y=Ge(J,300,Q),Z=m(()=>!!(w!=null&&w.enteringSubmenuRef.value)),H=_(!1);L(le,{enteringSubmenuRef:H});function M(){H.value=!0}function U(){H.value=!1}function k(){const{parentKey:o,tmNode:u}=e;u.disabled||p.value&&(r.value=o,i.value=null,d.value=u.key)}function l(){const{tmNode:o}=e;o.disabled||p.value&&d.value!==o.key&&k()}function s(o){if(e.tmNode.disabled||!p.value)return;const{relatedTarget:u}=o;u&&!de({target:u},"dropdownOption")&&!de({target:u},"scrollbarRail")&&(d.value=null)}function c(){const{value:o}=W,{tmNode:u}=e;p.value&&!o&&!u.disabled&&(n.doSelect(u.key,u.rawNode),n.doUpdateShow(!1))}return{labelField:y,renderLabel:g,renderIcon:x,siblingHasIcon:R.showIconRef,siblingHasSubmenu:R.hasSubmenuRef,menuProps:I,popoverBody:A,animated:v,mergedShowSubmenu:m(()=>Y.value&&!Z.value),rawNode:E,hasSubmenu:W,pending:q(()=>{const{value:o}=t,{key:u}=e.tmNode;return o.includes(u)}),childActive:q(()=>{const{value:o}=f,{key:u}=e.tmNode,b=o.findIndex(F=>u===F);return b===-1?!1:b<o.length-1}),active:q(()=>{const{value:o}=f,{key:u}=e.tmNode,b=o.findIndex(F=>u===F);return b===-1?!1:b===o.length-1}),mergedDisabled:X,renderOption:S,nodeProps:P,handleClick:c,handleMouseMove:l,handleMouseEnter:k,handleMouseLeave:s,handleSubmenuBeforeEnter:M,handleSubmenuAfterEnter:U}},render(){var e,n;const{animated:d,rawNode:i,mergedShowSubmenu:r,clsPrefix:t,siblingHasIcon:f,siblingHasSubmenu:v,renderLabel:p,renderIcon:g,renderOption:x,nodeProps:y,props:N,scrollable:S}=this;let P=null;if(r){const A=(e=this.menuProps)===null||e===void 0?void 0:e.call(this,i,i.children);P=a(me,Object.assign({},A,{clsPrefix:t,scrollable:this.scrollable,tmNodes:this.tmNode.children,parentKey:this.tmNode.key}))}const I={class:[`${t}-dropdown-option-body`,this.pending&&`${t}-dropdown-option-body--pending`,this.active&&`${t}-dropdown-option-body--active`,this.childActive&&`${t}-dropdown-option-body--child-active`,this.mergedDisabled&&`${t}-dropdown-option-body--disabled`],onMousemove:this.handleMouseMove,onMouseenter:this.handleMouseEnter,onMouseleave:this.handleMouseLeave,onClick:this.handleClick},w=y==null?void 0:y(i),R=a("div",Object.assign({class:[`${t}-dropdown-option`,w==null?void 0:w.class],"data-dropdown-option":!0},w),a("div",ue(I,N),[a("div",{class:[`${t}-dropdown-option-body__prefix`,f&&`${t}-dropdown-option-body__prefix--show-icon`]},[g?g(i):G(i.icon)]),a("div",{"data-dropdown-option":!0,class:`${t}-dropdown-option-body__label`},p?p(i):G((n=i[this.labelField])!==null&&n!==void 0?n:i.title)),a("div",{"data-dropdown-option":!0,class:[`${t}-dropdown-option-body__suffix`,v&&`${t}-dropdown-option-body__suffix--has-submenu`]},this.hasSubmenu?a(Le,null,{default:()=>a(We,null)}):null)]),this.hasSubmenu?a(Ne,null,{default:()=>[a(Re,null,{default:()=>a("div",{class:`${t}-dropdown-offset-container`},a(Ce,{show:this.mergedShowSubmenu,placement:this.placement,to:S&&this.popoverBody||void 0,teleportDisabled:!S},{default:()=>a("div",{class:`${t}-dropdown-menu-wrapper`},d?a(Oe,{onBeforeEnter:this.handleSubmenuBeforeEnter,onAfterEnter:this.handleSubmenuAfterEnter,name:"fade-in-scale-up-transition",appear:!0},{default:()=>P}):P)}))})]}):null);return x?x({node:R,option:i}):R}}),oo=$({name:"NDropdownGroup",props:{clsPrefix:{type:String,required:!0},tmNode:{type:Object,required:!0},parentKey:{type:[String,Number],default:null}},render(){const{tmNode:e,parentKey:n,clsPrefix:d}=this,{children:i}=e;return a(Fe,null,a(Ye,{clsPrefix:d,tmNode:e,key:e.key}),i==null?void 0:i.map(r=>{const{rawNode:t}=r;return t.show===!1?null:ve(t)?a(fe,{clsPrefix:d,key:r.key}):r.isGroup?(Be("dropdown","`group` node is not allowed to be put in `group` node."),null):a(he,{clsPrefix:d,tmNode:r,parentKey:n,key:r.key})}))}}),no=$({name:"DropdownRenderOption",props:{tmNode:{type:Object,required:!0}},render(){const{rawNode:{render:e,props:n}}=this.tmNode;return a("div",n,[e==null?void 0:e()])}}),me=$({name:"DropdownMenu",props:{scrollable:Boolean,showArrow:Boolean,arrowStyle:[String,Object],clsPrefix:{type:String,required:!0},tmNodes:{type:Array,default:()=>[]},parentKey:{type:[String,Number],default:null}},setup(e){const{renderIconRef:n,childrenFieldRef:d}=D(V);L(re,{showIconRef:m(()=>{const r=n.value;return e.tmNodes.some(t=>{var f;if(t.isGroup)return(f=t.children)===null||f===void 0?void 0:f.some(({rawNode:p})=>r?r(p):p.icon);const{rawNode:v}=t;return r?r(v):v.icon})}),hasSubmenuRef:m(()=>{const{value:r}=d;return e.tmNodes.some(t=>{var f;if(t.isGroup)return(f=t.children)===null||f===void 0?void 0:f.some(({rawNode:p})=>ne(p,r));const{rawNode:v}=t;return ne(v,r)})})});const i=_(null);return L(_e,null),L(De,null),L(ce,i),{bodyRef:i}},render(){const{parentKey:e,clsPrefix:n,scrollable:d}=this,i=this.tmNodes.map(r=>{const{rawNode:t}=r;return t.show===!1?null:eo(t)?a(no,{tmNode:r,key:r.key}):ve(t)?a(fe,{clsPrefix:n,key:r.key}):Ze(t)?a(oo,{clsPrefix:n,tmNode:r,parentKey:e,key:r.key}):a(he,{clsPrefix:n,tmNode:r,parentKey:e,key:r.key,props:t.props,scrollable:d})});return a("div",{class:[`${n}-dropdown-menu`,d&&`${n}-dropdown-menu--scrollable`],ref:"bodyRef"},d?a(Te,{contentClass:`${n}-dropdown-menu__content`},{default:()=>i}):i,this.showArrow?Ie({clsPrefix:n,arrowStyle:this.arrowStyle,arrowClass:void 0,arrowWrapperClass:void 0,arrowWrapperStyle:void 0}):null)}}),to=C("dropdown-menu",`
 transform-origin: var(--v-transform-origin);
 background-color: var(--n-color);
 border-radius: var(--n-border-radius);
 box-shadow: var(--n-box-shadow);
 position: relative;
 transition:
 background-color .3s var(--n-bezier),
 box-shadow .3s var(--n-bezier);
`,[$e(),C("dropdown-option",`
 position: relative;
 `,[j("a",`
 text-decoration: none;
 color: inherit;
 outline: none;
 `,[j("&::before",`
 content: "";
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 `)]),C("dropdown-option-body",`
 display: flex;
 cursor: pointer;
 position: relative;
 height: var(--n-option-height);
 line-height: var(--n-option-height);
 font-size: var(--n-font-size);
 color: var(--n-option-text-color);
 transition: color .3s var(--n-bezier);
 `,[j("&::before",`
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
 `),j("&::before","background-color: var(--n-option-color-hover);")]),K("active",`
 color: var(--n-option-text-color-active);
 `,[O("prefix, suffix",`
 color: var(--n-option-text-color-active);
 `),j("&::before","background-color: var(--n-option-color-active);")]),K("child-active",`
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
 `),C("icon",`
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
 `),C("icon",`
 font-size: var(--n-option-icon-size);
 `)]),C("dropdown-menu","pointer-events: all;")]),C("dropdown-offset-container",`
 pointer-events: none;
 position: absolute;
 left: 0;
 right: 0;
 top: -4px;
 bottom: -4px;
 `)]),C("dropdown-divider",`
 transition: background-color .3s var(--n-bezier);
 background-color: var(--n-divider-color);
 height: 1px;
 margin: 4px 0;
 `),C("dropdown-menu-wrapper",`
 transform-origin: var(--v-transform-origin);
 width: fit-content;
 `),j(">",[C("scrollbar",`
 height: inherit;
 max-height: inherit;
 `)]),ie("scrollable",`
 padding: var(--n-padding);
 `),K("scrollable",[O("content",`
 padding: var(--n-padding);
 `)])]),ro={animated:{type:Boolean,default:!0},keyboard:{type:Boolean,default:!0},size:String,inverted:Boolean,placement:{type:String,default:"bottom"},onSelect:[Function,Array],options:{type:Array,default:()=>[]},menuProps:Function,showArrow:Boolean,renderLabel:Function,renderIcon:Function,renderOption:Function,nodeProps:Function,labelField:{type:String,default:"label"},keyField:{type:String,default:"key"},childrenField:{type:String,default:"children"},value:[String,Number]},io=Object.keys(ae),lo=Object.assign(Object.assign(Object.assign({},ae),ro),pe.props),ho=$({name:"Dropdown",inheritAttrs:!1,props:lo,setup(e){const n=_(!1),d=Ue(z(e,"show"),n),i=m(()=>{const{keyField:l,childrenField:s}=e;return Ee(e.options,{getKey(c){return c[l]},getDisabled(c){return c.disabled===!0},getIgnored(c){return c.type==="divider"||c.type==="render"},getChildren(c){return c[s]}})}),r=m(()=>i.value.treeNodes),t=_(null),f=_(null),v=_(null),p=m(()=>{var l,s,c;return(c=(s=(l=t.value)!==null&&l!==void 0?l:f.value)!==null&&s!==void 0?s:v.value)!==null&&c!==void 0?c:null}),g=m(()=>i.value.getPath(p.value).keyPath),x=m(()=>i.value.getPath(e.value).keyPath),y=q(()=>e.keyboard&&d.value);qe({keydown:{ArrowUp:{prevent:!0,handler:Q},ArrowRight:{prevent:!0,handler:J},ArrowDown:{prevent:!0,handler:Y},ArrowLeft:{prevent:!0,handler:X},Enter:{prevent:!0,handler:Z},Escape:W}},y);const{mergedClsPrefixRef:N,inlineThemeDisabled:S,mergedComponentPropsRef:P}=Me(e),I=m(()=>{var l,s;return e.size||((s=(l=P==null?void 0:P.value)===null||l===void 0?void 0:l.Dropdown)===null||s===void 0?void 0:s.size)||"medium"}),w=pe("Dropdown","-dropdown",to,Qe,e,N);L(V,{labelFieldRef:z(e,"labelField"),childrenFieldRef:z(e,"childrenField"),renderLabelRef:z(e,"renderLabel"),renderIconRef:z(e,"renderIcon"),hoverKeyRef:t,keyboardKeyRef:f,lastToggledSubmenuKeyRef:v,pendingKeyPathRef:g,activeKeyPathRef:x,animatedRef:z(e,"animated"),mergedShowRef:d,nodePropsRef:z(e,"nodeProps"),renderOptionRef:z(e,"renderOption"),menuPropsRef:z(e,"menuProps"),doSelect:R,doUpdateShow:A}),se(d,l=>{!e.animated&&!l&&E()});function R(l,s){const{onSelect:c}=e;c&&oe(c,l,s)}function A(l){const{"onUpdate:show":s,onUpdateShow:c}=e;s&&oe(s,l),c&&oe(c,l),n.value=l}function E(){t.value=null,f.value=null,v.value=null}function W(){A(!1)}function X(){M("left")}function J(){M("right")}function Q(){M("up")}function Y(){M("down")}function Z(){const l=H();l!=null&&l.isLeaf&&d.value&&(R(l.key,l.rawNode),A(!1))}function H(){var l;const{value:s}=i,{value:c}=p;return!s||c===null?null:(l=s.getNode(c))!==null&&l!==void 0?l:null}function M(l){const{value:s}=p,{value:{getFirstAvailableNode:c}}=i;let o=null;if(s===null){const u=c();u!==null&&(o=u.key)}else{const u=H();if(u){let b;switch(l){case"down":b=u.getNext();break;case"up":b=u.getPrev();break;case"right":b=u.getChild();break;case"left":b=u.getParent();break}b&&(o=b.key)}}o!==null&&(t.value=null,f.value=o)}const U=m(()=>{const{inverted:l}=e,s=I.value,{common:{cubicBezierEaseInOut:c},self:o}=w.value,{padding:u,dividerColor:b,borderRadius:F,optionOpacityDisabled:ee,[T("optionIconSuffixWidth",s)]:B,[T("optionSuffixWidth",s)]:be,[T("optionIconPrefixWidth",s)]:we,[T("optionPrefixWidth",s)]:ge,[T("fontSize",s)]:xe,[T("optionHeight",s)]:ye,[T("optionIconSize",s)]:Se}=o,h={"--n-bezier":c,"--n-font-size":xe,"--n-padding":u,"--n-border-radius":F,"--n-option-height":ye,"--n-option-prefix-width":ge,"--n-option-icon-prefix-width":we,"--n-option-suffix-width":be,"--n-option-icon-suffix-width":B,"--n-option-icon-size":Se,"--n-divider-color":b,"--n-option-opacity-disabled":ee};return l?(h["--n-color"]=o.colorInverted,h["--n-option-color-hover"]=o.optionColorHoverInverted,h["--n-option-color-active"]=o.optionColorActiveInverted,h["--n-option-text-color"]=o.optionTextColorInverted,h["--n-option-text-color-hover"]=o.optionTextColorHoverInverted,h["--n-option-text-color-active"]=o.optionTextColorActiveInverted,h["--n-option-text-color-child-active"]=o.optionTextColorChildActiveInverted,h["--n-prefix-color"]=o.prefixColorInverted,h["--n-suffix-color"]=o.suffixColorInverted,h["--n-group-header-text-color"]=o.groupHeaderTextColorInverted):(h["--n-color"]=o.color,h["--n-option-color-hover"]=o.optionColorHover,h["--n-option-color-active"]=o.optionColorActive,h["--n-option-text-color"]=o.optionTextColor,h["--n-option-text-color-hover"]=o.optionTextColorHover,h["--n-option-text-color-active"]=o.optionTextColorActive,h["--n-option-text-color-child-active"]=o.optionTextColorChildActive,h["--n-prefix-color"]=o.prefixColor,h["--n-suffix-color"]=o.suffixColor,h["--n-group-header-text-color"]=o.groupHeaderTextColor),h}),k=S?je("dropdown",m(()=>`${I.value[0]}${e.inverted?"i":""}`),U,e):void 0;return{mergedClsPrefix:N,mergedTheme:w,mergedSize:I,tmNodes:r,mergedShow:d,handleAfterLeave:()=>{e.animated&&E()},doUpdateShow:A,cssVars:S?void 0:U,themeClass:k==null?void 0:k.themeClass,onRender:k==null?void 0:k.onRender}},render(){const e=(i,r,t,f,v)=>{var p;const{mergedClsPrefix:g,menuProps:x}=this;(p=this.onRender)===null||p===void 0||p.call(this);const y=(x==null?void 0:x(void 0,this.tmNodes.map(S=>S.rawNode)))||{},N={ref:Ve(r),class:[i,`${g}-dropdown`,`${g}-dropdown--${this.mergedSize}-size`,this.themeClass],clsPrefix:g,tmNodes:this.tmNodes,style:[...t,this.cssVars],showArrow:this.showArrow,arrowStyle:this.arrowStyle,scrollable:this.scrollable,onMouseenter:f,onMouseleave:v};return a(me,ue(this.$attrs,N,y))},{mergedTheme:n}=this,d={show:this.mergedShow,theme:n.peers.Popover,themeOverrides:n.peerOverrides.Popover,internalOnAfterLeave:this.handleAfterLeave,internalRenderBody:e,onUpdateShow:this.doUpdateShow,"onUpdate:show":void 0};return a(ke,Object.assign({},He(this.$props,io),d),{trigger:()=>{var i,r;return(r=(i=this.$slots).default)===null||r===void 0?void 0:r.call(i)}})}});export{ho as N,Ve as c,Qe as d};
