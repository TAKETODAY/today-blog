import { Effect, Reducer } from 'umi';
import { DashboardStatistics } from './data.d';
import request from "umi-request";

async function queryStatistics() {
  return request('/api/statistics/dashboard');
}

export interface ModalState {
  statistics?: DashboardStatistics;
}

export interface ModelType {
  namespace: string;
  state: ModalState;
  reducers: {
    save: Reducer<ModalState>;
    clear: Reducer<ModalState>;
  };
  effects: {
    init: Effect;
    fetchStatistics: Effect;
  };
}

const Model: ModelType = {
  namespace: 'dashboardAndworkplace',
  state: {
    statistics: undefined,
  },
  effects: {
    *init(_, { put }) {
      yield put({ type: 'fetchStatistics' });
    },
    *fetchStatistics(_, { call, put }) {
      const response = yield call(queryStatistics);
      yield put({
        type: 'save',
        payload: {
          statistics: response,
        },
      });
    },
  },
  reducers: {
    save(state, { payload }) {
      return {
        ...state,
        ...payload,
      };
    },
    clear() {
      return {
        statistics: undefined,
      };
    },
  },
};

export default Model;
