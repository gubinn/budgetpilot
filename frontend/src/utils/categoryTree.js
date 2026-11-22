// 分类树工具：支持一个父分类下同时挂支出/收入子分类（如「往来款」）

export function filterTreeByType(nodes, type) {
  const result = []
  for (const node of nodes || []) {
    const children = node.children ? filterTreeByType(node.children, type) : []
    // 自身类型匹配，或只是作为匹配子分类的父级容器
    if (node.type === type || children.length) {
      result.push({ ...node, children })
    }
  }
  return result
}

export function toCascaderOption(node) {
  const option = { label: node.name, value: node.id }
  if (node.children?.length) {
    option.children = node.children.map(toCascaderOption)
  }
  return option
}