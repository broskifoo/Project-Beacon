//! Task scheduler for background operations

use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;
use tokio::task::JoinHandle;
use tokio::time::{Duration, interval};

/// Handle for a scheduled task
#[derive(Debug, Clone)]
pub struct TaskHandle {
    pub id: String,
    pub name: String,
}

/// Scheduled task definition
pub struct ScheduledTask {
    pub name: String,
    pub interval: Duration,
    pub task: Box<dyn Fn() -> std::pin::Pin<Box<dyn std::future::Future<Output = ()> + Send>> + Send + Sync>,
}

/// Task scheduler for periodic background operations
pub struct TaskScheduler {
    tasks: Arc<RwLock<HashMap<String, JoinHandle<()>>>>,
    running: Arc<RwLock<bool>>,
}

impl Default for TaskScheduler {
    fn default() -> Self {
        Self::new()
    }
}

impl TaskScheduler {
    pub fn new() -> Self {
        Self {
            tasks: Arc::new(RwLock::new(HashMap::new())),
            running: Arc::new(RwLock::new(false)),
        }
    }

    /// Schedule a periodic task
    pub async fn schedule_periodic<F, Fut>(
        &self,
        name: &str,
        interval: Duration,
        task: F,
    ) -> Result<TaskHandle, SchedulerError>
    where
        F: Fn() -> Fut + Send + Sync + 'static,
        Fut: std::future::Future<Output = ()> + Send + 'static,
    {
        let mut tasks = self.tasks.write().await;
        
        if tasks.contains_key(name) {
            return Err(SchedulerError::TaskExists(name.to_string()));
        }

        let mut interval_timer = interval(interval);
        let name_owned = name.to_string();
        
        let handle = tokio::spawn(async move {
            loop {
                interval_timer.tick().await;
                task().await;
            }
        });

        let task_handle = TaskHandle {
            id: uuid::Uuid::new_v4().to_string(),
            name: name_owned.clone(),
        };

        tasks.insert(name_owned, handle);
        
        Ok(task_handle)
    }

    /// Schedule a one-time delayed task
    pub async fn schedule_delayed<F, Fut>(
        &self,
        name: &str,
        delay: Duration,
        task: F,
    ) -> Result<TaskHandle, SchedulerError>
    where
        F: FnOnce() -> Fut + Send + 'static,
        Fut: std::future::Future<Output = ()> + Send + 'static,
    {
        let mut tasks = self.tasks.write().await;
        let name_owned = name.to_string();
        
        let handle = tokio::spawn(async move {
            tokio::time::sleep(delay).await;
            task().await;
        });

        let task_handle = TaskHandle {
            id: uuid::Uuid::new_v4().to_string(),
            name: name_owned.clone(),
        };

        tasks.insert(name_owned, handle);
        
        Ok(task_handle)
    }

    /// Cancel a scheduled task
    pub async fn cancel_task(&self, name: &str) -> Result<(), SchedulerError> {
        let mut tasks = self.tasks.write().await;
        
        if let Some(handle) = tasks.remove(name) {
            handle.abort();
            Ok(())
        } else {
            Err(SchedulerError::TaskNotFound(name.to_string()))
        }
    }

    /// Cancel all tasks
    pub async fn shutdown(&self) -> Result<(), SchedulerError> {
        let mut tasks = self.tasks.write().await;
        
        for (_, handle) in tasks.drain() {
            handle.abort();
        }
        
        Ok(())
    }

    /// Get list of running tasks
    pub async fn list_tasks(&self) -> Vec<TaskHandle> {
        let tasks = self.tasks.read().await;
        tasks.keys().map(|name| TaskHandle {
            id: String::new(),
            name: name.clone(),
        }).collect()
    }
}

#[derive(Debug, thiserror::Error)]
pub enum SchedulerError {
    #[error("Task already exists: {0}")]
    TaskExists(String),
    
    #[error("Task not found: {0}")]
    TaskNotFound(String),
    
    #[error("Scheduler not running")]
    NotRunning,
}