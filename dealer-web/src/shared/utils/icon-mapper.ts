import {
  Activity,
  BarChart3,
  BookOpen,
  Box,
  Briefcase,
  Calendar,
  Car,
  ClipboardList,
  CreditCard,
  FileText,
  FolderOpen,
  Home,
  LayoutDashboard,
  Package,
  Percent,
  Settings,
  ShoppingBag,
  Tag,
  Truck,
  Users,
  Warehouse,
  type Component as LucideIcon,
} from '@lucide/vue'
import { type Component, markRaw } from 'vue'

/**
 * Maps Element Plus icon names (returned by backend) to lucide-vue-next components.
 * Add new mappings as needed.
 */
const iconMap: Record<string, Component> = {
  User: markRaw(Users),
  Users: markRaw(Users),
  Setting: markRaw(Settings),
  Settings: markRaw(Settings),
  Home: markRaw(Home),
  House: markRaw(Home),
  Menu: markRaw(LayoutDashboard),
  Grid: markRaw(LayoutDashboard),
  Monitor: markRaw(LayoutDashboard),
  DataLine: markRaw(BarChart3),
  DataAnalysis: markRaw(BarChart3),
  TrendCharts: markRaw(BarChart3),
  Goods: markRaw(Package),
  GoodsFilled: markRaw(Package),
  Box: markRaw(Box),
  ShoppingCart: markRaw(ShoppingBag),
  ShoppingCartFull: markRaw(ShoppingBag),
  Van: markRaw(Truck),
  Tickets: markRaw(ClipboardList),
  List: markRaw(ClipboardList),
  Notebook: markRaw(FileText),
  Document: markRaw(FileText),
  Calendar: markRaw(Calendar),
  Promotion: markRaw(Percent),
  Discount: markRaw(Percent),
  Coin: markRaw(CreditCard),
  Money: markRaw(CreditCard),
  Collection: markRaw(FolderOpen),
  Folder: markRaw(FolderOpen),
  Opportunity: markRaw(Briefcase),
  Briefcase: markRaw(Briefcase),
  Coordinate: markRaw(Activity),
  Connection: markRaw(Activity),
  Reading: markRaw(BookOpen),
  School: markRaw(BookOpen),
  Car: markRaw(Car),
  Truck: markRaw(Truck),
  Shop: markRaw(ShoppingBag),
  Warehouse: markRaw(Warehouse),
  Tag: markRaw(Tag),
}

const defaultIcon = markRaw(Box)

/**
 * Resolve an Element Plus icon name string to a lucide component.
 */
export function resolveIcon(iconName?: string): Component {
  if (!iconName) return defaultIcon
  return iconMap[iconName] ?? defaultIcon
}
