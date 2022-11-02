import header from 'src/components/nav-header'
import {
  ActionSheet,
  Button,
  Card,
  Cell,
  CellGroup,
  Checkbox,
  CheckboxGroup,
  Col,
  Dialog,
  Empty,
  Field,
  Icon,
  Image,
  List,
  Loading,
  NavBar,
  Picker,
  Popup,
  Radio,
  RadioGroup,
  Row,
  Sidebar,
  SidebarItem,
  Skeleton,
  Step,
  Stepper,
  Steps,
  SubmitBar,
  Swipe,
  SwipeCell,
  SwipeItem,
  Tab,
  Tabs,
  Tag,
  Toast,
  Uploader
} from 'vant';
import { Navigation, Skeleton as MySkeleton } from 'src/components'


const components = [
  Skeleton,
  Loading,
  Image,
  Tag,
  Col,
  Icon,
  Cell,
  Empty,
  CellGroup,
  Swipe,
  SwipeItem,
  ActionSheet,
  Sidebar,
  SidebarItem,
  Card,
  Button,
  SwipeCell,
  Dialog,
  header,
  Tab, Tabs, Toast, Row,
  Checkbox,
  CheckboxGroup,
  SubmitBar, NavBar,
  Navigation, List, Step, Steps, Field,
  MySkeleton,
  Popup,
  Stepper, RadioGroup, Radio, Picker, Uploader,
]


export default (Vue) => {
  components.forEach(Component => {
    Vue.component(Component.name, Component)
  });
}
