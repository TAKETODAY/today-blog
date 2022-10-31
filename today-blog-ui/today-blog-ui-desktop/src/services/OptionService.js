import { http, getCacheable } from '../utils';

export default class OptionService {

  options = {
    'site.icp': '蜀ICP备17031147号',
    'site.copyright': 'Copyright © TODAY & 2017 - 2021 All Rights Reserved.',
    'site.otherFooter': ''
  }

  getOptionsMap() {
    return getCacheable("/api/options").then(res => {
      this.options = res.data
    })
  }

  getOptions() {
    return http.get(`/api/options`)
  }

}

