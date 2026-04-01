
export function currency(price, decimals) {
  const priceArr = (price / 100).toFixed(2).split('.');
  const decimalStr =  `.${priceArr[1]}`;
  return `￥${priceArr[0]}${decimalStr}`
}
