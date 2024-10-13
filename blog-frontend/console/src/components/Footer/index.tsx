import { useOptions } from "@/components/hooks";

export default () => {
  // @ts-ignore
  const [options] = useOptions()
  return (
      <footer className="ant-layout-footer" style={{ padding: 0 }}>
        <div className="ant-pro-global-footer">
          <div className="ant-pro-global-footer-links">
            <a title="Harry Yang" target="_blank" href="https://taketoday.cn" rel="noreferrer">由 Harry Yang 提供技术支持</a>
          </div>
          <div className="ant-pro-global-footer-copyright">
            {options['site.copyright']}
          </div>
        </div>
      </footer>
  )
}
//
// export default () => {
//   const [options] = useOptions()
//   return (
//       <DefaultFooter
//           copyright={options['site.copyright']}
//           links={[
//             {
//               key: 'TODAY BLOG',
//               title: 'Powered By TODAY',
//               href: 'https://taketoday.cn',
//               blankTarget: true,
//             }
//           ]}
//       />
//   )
//   // (
//   //     <footer className="ant-layout-footer" style={{ padding: 0 }}>
//   //       <div className="ant-pro-global-footer">
//   //         <div className="ant-pro-global-footer-links">
//   //           <a title="Harry Yang" target="_blank" href="https://taketoday.cn" rel="noreferrer">由 Harry Yang 提供技术支持</a>
//   //         </div>
//   //         <div className="ant-pro-global-footer-copyright">
//   //           {options['site.copyright']}
//   //         </div>
//   //       </div>
//   //     </footer>
//   //   )
// }
