import {
  Activity,
  BarChart3,
  Building2,
  BookOpen,
  Box,
  Briefcase,
  Calendar,
  Car,
  CircleDollarSign,
  ClipboardList,
  CreditCard,
  FileText,
  FolderOpen,
  Gauge,
  Home,
  LayoutDashboard,
  ListTree,
  Magnet,
  Package,
  Paperclip,
  Percent,
  Settings,
  ShoppingBag,
  Tag,
  Truck,
  UserCog,
  UserSearch,
  Users,
  Warehouse,
  WalletCards,
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
  Gauge: markRaw(Gauge),
  Menu: markRaw(LayoutDashboard),
  Grid: markRaw(BookOpen),
  Monitor: markRaw(LayoutDashboard),
  DataLine: markRaw(BarChart3),
  DataAnalysis: markRaw(BarChart3),
  TrendCharts: markRaw(BarChart3),
  Goods: markRaw(Package),
  GoodsFilled: markRaw(Package),
  Memo: markRaw(Package),
  SetUp: markRaw(Car),
  Box: markRaw(Box),
  ShoppingCart: markRaw(ShoppingBag),
  ShoppingCartFull: markRaw(ShoppingBag),
  Van: markRaw(Truck),
  Tickets: markRaw(ClipboardList),
  List: markRaw(ClipboardList),
  ListTree: markRaw(ListTree),
  Notebook: markRaw(FileText),
  Document: markRaw(FileText),
  Calendar: markRaw(Calendar),
  OfficeBuilding: markRaw(Building2),
  Promotion: markRaw(Percent),
  Discount: markRaw(Percent),
  BadgePercent: markRaw(Percent),
  Coin: markRaw(CircleDollarSign),
  Money: markRaw(CircleDollarSign),
  Wallet: markRaw(WalletCards),
  Collection: markRaw(FolderOpen),
  Folder: markRaw(FolderOpen),
  Opportunity: markRaw(Briefcase),
  Briefcase: markRaw(Briefcase),
  Coordinate: markRaw(Activity),
  Connection: markRaw(Activity),
  Magnet: markRaw(UserSearch),
  Paperclip: markRaw(Paperclip),
  Postcard: markRaw(BookOpen),
  Reading: markRaw(BookOpen),
  School: markRaw(BookOpen),
  Stamp: markRaw(UserCog),
  UserCog: markRaw(UserCog),
  UserFilled: markRaw(Users),
  Car: markRaw(Car),
  Truck: markRaw(Truck),
  Shop: markRaw(ShoppingBag),
  Warehouse: markRaw(Warehouse),
  Tag: markRaw(Tag),
}

const defaultIcon = markRaw(LayoutDashboard)

/**
 * Resolve an Element Plus icon name string to a lucide component.
 */
export function resolveIcon(iconName?: string): Component {
  if (!iconName) return defaultIcon
  return iconMap[iconName] ?? defaultIcon
}
