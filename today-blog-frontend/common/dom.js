
function scrollTo(anchorName, options) {
  if (anchorName) {
    let anchorElement = document.querySelector(anchorName);
    if (anchorElement) {
      anchorElement.scrollIntoView(Object.assign({}, options, { block: 'center', behavior: 'smooth' }));
    }
  }
}

function scrollTop(top = 0) {
  document.documentElement.scrollTop = document.body.scrollTop = top
}

export { scrollTo, scrollTop }

