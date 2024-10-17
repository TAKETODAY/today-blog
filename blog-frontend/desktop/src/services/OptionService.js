import { http, getCacheable } from 'core';

export default class OptionService {

  options = {
    'site.icp': '',
    'site.copyright': 'Copyright © TODAY & 2017 - 2023 All Rights Reserved.',
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

