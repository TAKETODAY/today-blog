import { createStore, applyMiddleware, compose } from 'redux';
import thunk from 'redux-thunk';
import reducers from './reducers';

let composeEnhancers
if (process.env.NODE_ENV === 'production') {
  composeEnhancers = compose
}
else {
  composeEnhancers = window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__ || compose
}

export const store = createStore(reducers, /* preloadedState, */ composeEnhancers(
    applyMiddleware(thunk)
));

// export const navigationsStore = createStore(
//   update,
//   applyMiddleware(thunk)
// );
