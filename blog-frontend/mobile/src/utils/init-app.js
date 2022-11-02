import { getCacheable, http } from "./http";
import { handleError } from "utils/error-handler";

const initStore = (store) => {

  http.get("/api/auth").then((res) => {
    store.dispatch("user/updateSession", res.data)
  })

  getCacheable("/api/options").then(res => {
    store.dispatch("options/updateOptions", res.data)
  }).catch(handleError)

  // getCacheable("/api/categories").then(res => {
  //   store.dispatch(updateCategories(res.data))
  //   store.dispatch("options/updateOptions", res.data)
  // })

  // getCacheable("/api/tags").then(res => {
  //   store.dispatch(updateLabels(res.data))
  // })

}

export default [
  initStore
]
