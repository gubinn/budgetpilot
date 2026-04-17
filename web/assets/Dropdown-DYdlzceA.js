import{p as ue,N as ce,e as re,B as ke,a as Oe,b as Ke,h as le,r as Te,c as ze}from"./Popover-BdF2mV9_.js";import{r as A,w as pe,d as T,h as l,B as fe,j as ve,E as _e,D as Ae,p as he,q as V,c as m,F as ie,O as G,J as j,Y as me,l as Be,Q as q,bm as be,H as M,R as Fe,aG as $e,bb as je,bn as De,bo as He,e as N,k as Le,a as L,L as ae,g as O,i as _,U as Me,x as We,K as ne,t as K,z as $}from"./index-B7k3i8Dy.js";import{N as Ee}from"./Icon-_INjptPX.js";import{u as Ue}from"./get-DX8-5hPx.js";import{u as qe}from"./use-keyboard-B6D0xAqD.js";function Ge(e,o,d){const n=A(e.value);let i=null;return pe(e,t=>{i!==null&&window.clearTimeout(i),t===!0?d&&!d.value?n.value=!0:i=window.setTimeout(()=>{n.value=!0},o):n.value=!1}),n}function Ve(e){return o=>{o?e.value=o.$el:e.value=null}}const Je=T({name:"ChevronRight",render(){return l("svg",{viewBox:"0 0 16 16",fill:"none",xmlns:"http://www.w3.org/2000/svg"},l("path",{d:"M5.64645 3.14645C5.45118 3.34171 5.45118 3.65829 5.64645 3.85355L9.79289 8L5.64645 12.1464C5.45118 12.3417 5.45118 12.6583 5.64645 12.8536C5.84171 13.0488 6.15829 13.0488 6.35355 12.8536L10.8536 8.35355C11.0488 8.15829 11.0488 7.84171 10.8536 7.64645L6.35355 3.14645C6.15829 2.95118 5.84171 2.95118 5.64645 3.14645Z",fill:"currentColor"}))}}),Qe={padding:"4px 0",optionIconSizeSmall:"14px",optionIconSizeMedium:"16px",optionIconSizeLarge:"16px",optionIconSizeHuge:"18px",optionSuffixWidthSmall:"14px",optionSuffixWidthMedium:"14px",optionSuffixWidthLarge:"16px",optionSuffixWidthHuge:"16px",optionIconSuffixWidthSmall:"32px",optionIconSuffixWidthMedium:"32px",optionIconSuffixWidthLarge:"36px",optionIconSuffixWidthHuge:"36px",optionPrefixWidthSmall:"14px",optionPrefixWidthMedium:"14px",optionPrefixWidthLarge:"16px",optionPrefixWidthHuge:"16px",optionIconPrefixWidthSmall:"36px",optionIconPrefixWidthMedium:"36px",optionIconPrefixWidthLarge:"40px",optionIconPrefixWidthHuge:"40px"};function Xe(e){const{primaryColor:o,textColor2:d,dividerColor:n,hoverColor:i,popoverColor:t,invertedColor:f,borderRadius:v,fontSizeSmall:p,fontSizeMedium:g,fontSizeLarge:x,fontSizeHuge:y,heightSmall:C,heightMedium:S,heightLarge:P,heightHuge:I,textColor3:w,opacityDisabled:R}=e;return Object.assign(Object.assign({},Qe),{optionHeightSmall:C,optionHeightMedium:S,optionHeightLarge:P,optionHeightHuge:I,borderRadius:v,fontSizeSmall:p,fontSizeMedium:g,fontSizeLarge:x,fontSizeHuge:y,optionTextColor:d,optionTextColorHover:d,optionTextColorActive:o,optionTextColorChildActive:o,color:t,dividerColor:n,suffixColor:d,prefixColor:d,optionColorHover:i,optionColorActive:_e(o,{alpha:.1}),groupHeaderTextColor:w,optionTextColorInverted:"#BBB",optionTextColorHoverInverted:"#FFF",optionTextColorActiveInverted:"#FFF",optionTextColorChildActiveInverted:"#FFF",colorInverted:f,dividerColorInverted:"#BBB",suffixColorInverted:"#BBB",prefixColorInverted:"#BBB",optionColorHoverInverted:o,optionColorActiveInverted:o,groupHeaderTextColorInverted:"#AAA",optionOpacityDisabled:R})}const Ye=fe({name:"Dropdown",common:ve,peers:{Popover:ue},self:Xe}),Ze={padding:"8px 14px"};function eo(e){const{borderRadius:o,boxShadow2:d,baseColor:n}=e;return Object.assign(Object.assign({},Ze),{borderRadius:o,boxShadow:d,color:Ae(n,"rgba(0, 0, 0, .85)"),textColor:n})}const oo=fe({name:"Tooltip",common:ve,peers:{Popover:ue},self:eo}),no=Object.assign(Object.assign({},re),V.props),wo=T({name:"Tooltip",props:no,slots:Object,__popover__:!0,setup(e){const{mergedClsPrefixRef:o}=he(e),d=V("Tooltip","-tooltip",void 0,oo,e,o),n=A(null);return Object.assign(Object.assign({},{syncPosition(){n.value.syncPosition()},setShow(t){n.value.setShow(t)}}),{popoverRef:n,mergedTheme:d,popoverThemeOverrides:m(()=>d.value.self)})},render(){const{mergedTheme:e,internalExtraClass:o}=this;return l(ce,Object.assign(Object.assign({},this.$props),{theme:e.peers.Popover,themeOverrides:e.peerOverrides.Popover,builtinThemeOverrides:this.popoverThemeOverrides,internalExtraClass:o.concat("tooltip"),ref:"popoverRef"}),this.$slots)}}),de=ie("n-dropdown-menu"),J=ie("n-dropdown"),se=ie("n-dropdown-option"),we=T({name:"DropdownDivider",props:{clsPrefix:{type:String,required:!0}},render(){return l("div",{class:`${this.clsPrefix}-dropdown-divider`})}}),to=T({name:"DropdownGroupHeader",props:{clsPrefix:{type:String,required:!0},tmNode:{type:Object,required:!0}},setup(){const{showIconRef:e,hasSubmenuRef:o}=j(de),{renderLabelRef:d,labelFieldRef:n,nodePropsRef:i,renderOptionRef:t}=j(J);return{labelField:n,showIcon:e,hasSubmenu:o,renderLabel:d,nodeProps:i,renderOption:t}},render(){var e;const{clsPrefix:o,hasSubmenu:d,showIcon:n,nodeProps:i,renderLabel:t,renderOption:f}=this,{rawNode:v}=this.tmNode,p=l("div",Object.assign({class:`${o}-dropdown-option`},i==null?void 0:i(v)),l("div",{class:`${o}-dropdown-option-body ${o}-dropdown-option-body--group`},l("div",{"data-dropdown-option":!0,class:[`${o}-dropdown-option-body__prefix`,n&&`${o}-dropdown-option-body__prefix--show-icon`]},G(v.icon)),l("div",{class:`${o}-dropdown-option-body__label`,"data-dropdown-option":!0},t?t(v):G((e=v.title)!==null&&e!==void 0?e:v[this.labelField])),l("div",{class:[`${o}-dropdown-option-body__suffix`,d&&`${o}-dropdown-option-body__suffix--has-submenu`],"data-dropdown-option":!0})));return f?f({node:p,option:v}):p}});function te(e,o){return e.type==="submenu"||e.type===void 0&&e[o]!==void 0}function ro(e){return e.type==="group"}function ge(e){return e.type==="divider"}function io(e){return e.type==="render"}const xe=T({name:"DropdownOption",props:{clsPrefix:{type:String,required:!0},tmNode:{type:Object,required:!0},parentKey:{type:[String,Number],default:null},placement:{type:String,default:"right-start"},props:Object,scrollable:Boolean},setup(e){const o=j(J),{hoverKeyRef:d,keyboardKeyRef:n,lastToggledSubmenuKeyRef:i,pendingKeyPathRef:t,activeKeyPathRef:f,animatedRef:v,mergedShowRef:p,renderLabelRef:g,renderIconRef:x,labelFieldRef:y,childrenFieldRef:C,renderOptionRef:S,nodePropsRef:P,menuPropsRef:I}=o,w=j(se,null),R=j(de),z=j(be),E=m(()=>e.tmNode.rawNode),W=m(()=>{const{value:r}=C;return te(e.tmNode.rawNode,r)}),Q=m(()=>{const{disabled:r}=e.tmNode;return r}),X=m(()=>{if(!W.value)return!1;const{key:r,disabled:u}=e.tmNode;if(u)return!1;const{value:b}=d,{value:B}=n,{value:oe}=i,{value:F}=t;return b!==null?F.includes(r):B!==null?F.includes(r)&&F[F.length-1]!==r:oe!==null?F.includes(r):!1}),Y=m(()=>n.value===null&&!v.value),Z=Ge(X,300,Y),ee=m(()=>!!(w!=null&&w.enteringSubmenuRef.value)),D=A(!1);M(se,{enteringSubmenuRef:D});function H(){D.value=!0}function U(){D.value=!1}function k(){const{parentKey:r,tmNode:u}=e;u.disabled||p.value&&(i.value=r,n.value=null,d.value=u.key)}function a(){const{tmNode:r}=e;r.disabled||p.value&&d.value!==r.key&&k()}function s(r){if(e.tmNode.disabled||!p.value)return;const{relatedTarget:u}=r;u&&!le({target:u},"dropdownOption")&&!le({target:u},"scrollbarRail")&&(d.value=null)}function c(){const{value:r}=W,{tmNode:u}=e;p.value&&!r&&!u.disabled&&(o.doSelect(u.key,u.rawNode),o.doUpdateShow(!1))}return{labelField:y,renderLabel:g,renderIcon:x,siblingHasIcon:R.showIconRef,siblingHasSubmenu:R.hasSubmenuRef,menuProps:I,popoverBody:z,animated:v,mergedShowSubmenu:m(()=>Z.value&&!ee.value),rawNode:E,hasSubmenu:W,pending:q(()=>{const{value:r}=t,{key:u}=e.tmNode;return r.includes(u)}),childActive:q(()=>{const{value:r}=f,{key:u}=e.tmNode,b=r.findIndex(B=>u===B);return b===-1?!1:b<r.length-1}),active:q(()=>{const{value:r}=f,{key:u}=e.tmNode,b=r.findIndex(B=>u===B);return b===-1?!1:b===r.length-1}),mergedDisabled:Q,renderOption:S,nodeProps:P,handleClick:c,handleMouseMove:a,handleMouseEnter:k,handleMouseLeave:s,handleSubmenuBeforeEnter:H,handleSubmenuAfterEnter:U}},render(){var e,o;const{animated:d,rawNode:n,mergedShowSubmenu:i,clsPrefix:t,siblingHasIcon:f,siblingHasSubmenu:v,renderLabel:p,renderIcon:g,renderOption:x,nodeProps:y,props:C,scrollable:S}=this;let P=null;if(i){const z=(e=this.menuProps)===null||e===void 0?void 0:e.call(this,n,n.children);P=l(ye,Object.assign({},z,{clsPrefix:t,scrollable:this.scrollable,tmNodes:this.tmNode.children,parentKey:this.tmNode.key}))}const I={class:[`${t}-dropdown-option-body`,this.pending&&`${t}-dropdown-option-body--pending`,this.active&&`${t}-dropdown-option-body--active`,this.childActive&&`${t}-dropdown-option-body--child-active`,this.mergedDisabled&&`${t}-dropdown-option-body--disabled`],onMousemove:this.handleMouseMove,onMouseenter:this.handleMouseEnter,onMouseleave:this.handleMouseLeave,onClick:this.handleClick},w=y==null?void 0:y(n),R=l("div",Object.assign({class:[`${t}-dropdown-option`,w==null?void 0:w.class],"data-dropdown-option":!0},w),l("div",me(I,C),[l("div",{class:[`${t}-dropdown-option-body__prefix`,f&&`${t}-dropdown-option-body__prefix--show-icon`]},[g?g(n):G(n.icon)]),l("div",{"data-dropdown-option":!0,class:`${t}-dropdown-option-body__label`},p?p(n):G((o=n[this.labelField])!==null&&o!==void 0?o:n.title)),l("div",{"data-dropdown-option":!0,class:[`${t}-dropdown-option-body__suffix`,v&&`${t}-dropdown-option-body__suffix--has-submenu`]},this.hasSubmenu?l(Ee,null,{default:()=>l(Je,null)}):null)]),this.hasSubmenu?l(ke,null,{default:()=>[l(Oe,null,{default:()=>l("div",{class:`${t}-dropdown-offset-container`},l(Ke,{show:this.mergedShowSubmenu,placement:this.placement,to:S&&this.popoverBody||void 0,teleportDisabled:!S},{default:()=>l("div",{class:`${t}-dropdown-menu-wrapper`},d?l(Be,{onBeforeEnter:this.handleSubmenuBeforeEnter,onAfterEnter:this.handleSubmenuAfterEnter,name:"fade-in-scale-up-transition",appear:!0},{default:()=>P}):P)}))})]}):null);return x?x({node:R,option:n}):R}}),lo=T({name:"NDropdownGroup",props:{clsPrefix:{type:String,required:!0},tmNode:{type:Object,required:!0},parentKey:{type:[String,Number],default:null}},render(){const{tmNode:e,parentKey:o,clsPrefix:d}=this,{children:n}=e;return l(Fe,null,l(to,{clsPrefix:d,tmNode:e,key:e.key}),n==null?void 0:n.map(i=>{const{rawNode:t}=i;return t.show===!1?null:ge(t)?l(we,{clsPrefix:d,key:i.key}):i.isGroup?($e("dropdown","`group` node is not allowed to be put in `group` node."),null):l(xe,{clsPrefix:d,tmNode:i,parentKey:o,key:i.key})}))}}),ao=T({name:"DropdownRenderOption",props:{tmNode:{type:Object,required:!0}},render(){const{rawNode:{render:e,props:o}}=this.tmNode;return l("div",o,[e==null?void 0:e()])}}),ye=T({name:"DropdownMenu",props:{scrollable:Boolean,showArrow:Boolean,arrowStyle:[String,Object],clsPrefix:{type:String,required:!0},tmNodes:{type:Array,default:()=>[]},parentKey:{type:[String,Number],default:null}},setup(e){const{renderIconRef:o,childrenFieldRef:d}=j(J);M(de,{showIconRef:m(()=>{const i=o.value;return e.tmNodes.some(t=>{var f;if(t.isGroup)return(f=t.children)===null||f===void 0?void 0:f.some(({rawNode:p})=>i?i(p):p.icon);const{rawNode:v}=t;return i?i(v):v.icon})}),hasSubmenuRef:m(()=>{const{value:i}=d;return e.tmNodes.some(t=>{var f;if(t.isGroup)return(f=t.children)===null||f===void 0?void 0:f.some(({rawNode:p})=>te(p,i));const{rawNode:v}=t;return te(v,i)})})});const n=A(null);return M(De,null),M(He,null),M(be,n),{bodyRef:n}},render(){const{parentKey:e,clsPrefix:o,scrollable:d}=this,n=this.tmNodes.map(i=>{const{rawNode:t}=i;return t.show===!1?null:io(t)?l(ao,{tmNode:i,key:i.key}):ge(t)?l(we,{clsPrefix:o,key:i.key}):ro(t)?l(lo,{clsPrefix:o,tmNode:i,parentKey:e,key:i.key}):l(xe,{clsPrefix:o,tmNode:i,parentKey:e,key:i.key,props:t.props,scrollable:d})});return l("div",{class:[`${o}-dropdown-menu`,d&&`${o}-dropdown-menu--scrollable`],ref:"bodyRef"},d?l(je,{contentClass:`${o}-dropdown-menu__content`},{default:()=>n}):n,this.showArrow?Te({clsPrefix:o,arrowStyle:this.arrowStyle,arrowClass:void 0,arrowWrapperClass:void 0,arrowWrapperStyle:void 0}):null)}}),so=N("dropdown-menu",`
 transform-origin: var(--v-transform-origin);
 background-color: var(--n-color);
 border-radius: var(--n-border-radius);
 box-shadow: var(--n-box-shadow);
 position: relative;
 transition:
 background-color .3s var(--n-bezier),
 box-shadow .3s var(--n-bezier);
`,[Le(),N("dropdown-option",`
 position: relative;
 `,[L("a",`
 text-decoration: none;
 color: inherit;
 outline: none;
 `,[L("&::before",`
 content: "";
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 `)]),N("dropdown-option-body",`
 display: flex;
 cursor: pointer;
 position: relative;
 height: var(--n-option-height);
 line-height: var(--n-option-height);
 font-size: var(--n-font-size);
 color: var(--n-option-text-color);
 transition: color .3s var(--n-bezier);
 `,[L("&::before",`
 content: "";
 position: absolute;
 top: 0;
 bottom: 0;
 left: 4px;
 right: 4px;
 transition: background-color .3s var(--n-bezier);
 border-radius: var(--n-border-radius);
 `),ae("disabled",[O("pending",`
 color: var(--n-option-text-color-hover);
 `,[_("prefix, suffix",`
 color: var(--n-option-text-color-hover);
 `),L("&::before","background-color: var(--n-option-color-hover);")]),O("active",`
 color: var(--n-option-text-color-active);
 `,[_("prefix, suffix",`
 color: var(--n-option-text-color-active);
 `),L("&::before","background-color: var(--n-option-color-active);")]),O("child-active",`
 color: var(--n-option-text-color-child-active);
 `,[_("prefix, suffix",`
 color: var(--n-option-text-color-child-active);
 `)])]),O("disabled",`
 cursor: not-allowed;
 opacity: var(--n-option-opacity-disabled);
 `),O("group",`
 font-size: calc(var(--n-font-size) - 1px);
 color: var(--n-group-header-text-color);
 `,[_("prefix",`
 width: calc(var(--n-option-prefix-width) / 2);
 `,[O("show-icon",`
 width: calc(var(--n-option-icon-prefix-width) / 2);
 `)])]),_("prefix",`
 width: var(--n-option-prefix-width);
 display: flex;
 justify-content: center;
 align-items: center;
 color: var(--n-prefix-color);
 transition: color .3s var(--n-bezier);
 z-index: 1;
 `,[O("show-icon",`
 width: var(--n-option-icon-prefix-width);
 `),N("icon",`
 font-size: var(--n-option-icon-size);
 `)]),_("label",`
 white-space: nowrap;
 flex: 1;
 z-index: 1;
 `),_("suffix",`
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
 `,[O("has-submenu",`
 width: var(--n-option-icon-suffix-width);
 `),N("icon",`
 font-size: var(--n-option-icon-size);
 `)]),N("dropdown-menu","pointer-events: all;")]),N("dropdown-offset-container",`
 pointer-events: none;
 position: absolute;
 left: 0;
 right: 0;
 top: -4px;
 bottom: -4px;
 `)]),N("dropdown-divider",`
 transition: background-color .3s var(--n-bezier);
 background-color: var(--n-divider-color);
 height: 1px;
 margin: 4px 0;
 `),N("dropdown-menu-wrapper",`
 transform-origin: var(--v-transform-origin);
 width: fit-content;
 `),L(">",[N("scrollbar",`
 height: inherit;
 max-height: inherit;
 `)]),ae("scrollable",`
 padding: var(--n-padding);
 `),O("scrollable",[_("content",`
 padding: var(--n-padding);
 `)])]),uo={animated:{type:Boolean,default:!0},keyboard:{type:Boolean,default:!0},size:String,inverted:Boolean,placement:{type:String,default:"bottom"},onSelect:[Function,Array],options:{type:Array,default:()=>[]},menuProps:Function,showArrow:Boolean,renderLabel:Function,renderIcon:Function,renderOption:Function,nodeProps:Function,labelField:{type:String,default:"label"},keyField:{type:String,default:"key"},childrenField:{type:String,default:"children"},value:[String,Number]},co=Object.keys(re),po=Object.assign(Object.assign(Object.assign({},re),uo),V.props),go=T({name:"Dropdown",inheritAttrs:!1,props:po,setup(e){const o=A(!1),d=Ue(K(e,"show"),o),n=m(()=>{const{keyField:a,childrenField:s}=e;return ze(e.options,{getKey(c){return c[a]},getDisabled(c){return c.disabled===!0},getIgnored(c){return c.type==="divider"||c.type==="render"},getChildren(c){return c[s]}})}),i=m(()=>n.value.treeNodes),t=A(null),f=A(null),v=A(null),p=m(()=>{var a,s,c;return(c=(s=(a=t.value)!==null&&a!==void 0?a:f.value)!==null&&s!==void 0?s:v.value)!==null&&c!==void 0?c:null}),g=m(()=>n.value.getPath(p.value).keyPath),x=m(()=>n.value.getPath(e.value).keyPath),y=q(()=>e.keyboard&&d.value);qe({keydown:{ArrowUp:{prevent:!0,handler:Y},ArrowRight:{prevent:!0,handler:X},ArrowDown:{prevent:!0,handler:Z},ArrowLeft:{prevent:!0,handler:Q},Enter:{prevent:!0,handler:ee},Escape:W}},y);const{mergedClsPrefixRef:C,inlineThemeDisabled:S,mergedComponentPropsRef:P}=he(e),I=m(()=>{var a,s;return e.size||((s=(a=P==null?void 0:P.value)===null||a===void 0?void 0:a.Dropdown)===null||s===void 0?void 0:s.size)||"medium"}),w=V("Dropdown","-dropdown",so,Ye,e,C);M(J,{labelFieldRef:K(e,"labelField"),childrenFieldRef:K(e,"childrenField"),renderLabelRef:K(e,"renderLabel"),renderIconRef:K(e,"renderIcon"),hoverKeyRef:t,keyboardKeyRef:f,lastToggledSubmenuKeyRef:v,pendingKeyPathRef:g,activeKeyPathRef:x,animatedRef:K(e,"animated"),mergedShowRef:d,nodePropsRef:K(e,"nodeProps"),renderOptionRef:K(e,"renderOption"),menuPropsRef:K(e,"menuProps"),doSelect:R,doUpdateShow:z}),pe(d,a=>{!e.animated&&!a&&E()});function R(a,s){const{onSelect:c}=e;c&&ne(c,a,s)}function z(a){const{"onUpdate:show":s,onUpdateShow:c}=e;s&&ne(s,a),c&&ne(c,a),o.value=a}function E(){t.value=null,f.value=null,v.value=null}function W(){z(!1)}function Q(){H("left")}function X(){H("right")}function Y(){H("up")}function Z(){H("down")}function ee(){const a=D();a!=null&&a.isLeaf&&d.value&&(R(a.key,a.rawNode),z(!1))}function D(){var a;const{value:s}=n,{value:c}=p;return!s||c===null?null:(a=s.getNode(c))!==null&&a!==void 0?a:null}function H(a){const{value:s}=p,{value:{getFirstAvailableNode:c}}=n;let r=null;if(s===null){const u=c();u!==null&&(r=u.key)}else{const u=D();if(u){let b;switch(a){case"down":b=u.getNext();break;case"up":b=u.getPrev();break;case"right":b=u.getChild();break;case"left":b=u.getParent();break}b&&(r=b.key)}}r!==null&&(t.value=null,f.value=r)}const U=m(()=>{const{inverted:a}=e,s=I.value,{common:{cubicBezierEaseInOut:c},self:r}=w.value,{padding:u,dividerColor:b,borderRadius:B,optionOpacityDisabled:oe,[$("optionIconSuffixWidth",s)]:F,[$("optionSuffixWidth",s)]:Se,[$("optionIconPrefixWidth",s)]:Pe,[$("optionPrefixWidth",s)]:Ce,[$("fontSize",s)]:Re,[$("optionHeight",s)]:Ne,[$("optionIconSize",s)]:Ie}=r,h={"--n-bezier":c,"--n-font-size":Re,"--n-padding":u,"--n-border-radius":B,"--n-option-height":Ne,"--n-option-prefix-width":Ce,"--n-option-icon-prefix-width":Pe,"--n-option-suffix-width":Se,"--n-option-icon-suffix-width":F,"--n-option-icon-size":Ie,"--n-divider-color":b,"--n-option-opacity-disabled":oe};return a?(h["--n-color"]=r.colorInverted,h["--n-option-color-hover"]=r.optionColorHoverInverted,h["--n-option-color-active"]=r.optionColorActiveInverted,h["--n-option-text-color"]=r.optionTextColorInverted,h["--n-option-text-color-hover"]=r.optionTextColorHoverInverted,h["--n-option-text-color-active"]=r.optionTextColorActiveInverted,h["--n-option-text-color-child-active"]=r.optionTextColorChildActiveInverted,h["--n-prefix-color"]=r.prefixColorInverted,h["--n-suffix-color"]=r.suffixColorInverted,h["--n-group-header-text-color"]=r.groupHeaderTextColorInverted):(h["--n-color"]=r.color,h["--n-option-color-hover"]=r.optionColorHover,h["--n-option-color-active"]=r.optionColorActive,h["--n-option-text-color"]=r.optionTextColor,h["--n-option-text-color-hover"]=r.optionTextColorHover,h["--n-option-text-color-active"]=r.optionTextColorActive,h["--n-option-text-color-child-active"]=r.optionTextColorChildActive,h["--n-prefix-color"]=r.prefixColor,h["--n-suffix-color"]=r.suffixColor,h["--n-group-header-text-color"]=r.groupHeaderTextColor),h}),k=S?We("dropdown",m(()=>`${I.value[0]}${e.inverted?"i":""}`),U,e):void 0;return{mergedClsPrefix:C,mergedTheme:w,mergedSize:I,tmNodes:i,mergedShow:d,handleAfterLeave:()=>{e.animated&&E()},doUpdateShow:z,cssVars:S?void 0:U,themeClass:k==null?void 0:k.themeClass,onRender:k==null?void 0:k.onRender}},render(){const e=(n,i,t,f,v)=>{var p;const{mergedClsPrefix:g,menuProps:x}=this;(p=this.onRender)===null||p===void 0||p.call(this);const y=(x==null?void 0:x(void 0,this.tmNodes.map(S=>S.rawNode)))||{},C={ref:Ve(i),class:[n,`${g}-dropdown`,`${g}-dropdown--${this.mergedSize}-size`,this.themeClass],clsPrefix:g,tmNodes:this.tmNodes,style:[...t,this.cssVars],showArrow:this.showArrow,arrowStyle:this.arrowStyle,scrollable:this.scrollable,onMouseenter:f,onMouseleave:v};return l(ye,me(this.$attrs,C,y))},{mergedTheme:o}=this,d={show:this.mergedShow,theme:o.peers.Popover,themeOverrides:o.peerOverrides.Popover,internalOnAfterLeave:this.handleAfterLeave,internalRenderBody:e,onUpdateShow:this.doUpdateShow,"onUpdate:show":void 0};return l(ce,Object.assign({},Me(this.$props,co),d),{trigger:()=>{var n,i;return(i=(n=this.$slots).default)===null||i===void 0?void 0:i.call(n)}})}});export{Je as C,wo as N,go as a,Ve as c,Ye as d,oo as t};
